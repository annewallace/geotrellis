package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.LayerIOError
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.util.cache.LRUCache

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io._
import org.apache.hadoop.fs.Path
import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}
import com.typesafe.config.ConfigFactory

import java.util.concurrent.Executors

class HadoopCollectionReader(maxOpenFiles: Int) {
  val readers = new LRUCache[Path, MapFile.Reader](maxOpenFiles.toLong, {x => 1l}) {
    override def evicted(reader: MapFile.Reader) = reader.close()
  }

  def read[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](path: Path,
    conf: Configuration,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    indexFilterOnly: Boolean,
    writerSchema: Option[Schema] = None,
    threads: Int = ConfigFactory.load().getInt("geotrellis.hadoop.threads.collection.read")): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)
    val indexRanges = queryKeyBounds.flatMap(decomposeBounds).toIterator

    val codec = KeyValueRecordCodec[K, V]

    val pathRanges: Vector[(Path, Long, Long)] =
      FilterMapFileInputFormat.layerRanges(path, conf)

    val pool = Executors.newFixedThreadPool(threads)

    val range: Process[Task, Iterator[Long]] = Process.unfold(indexRanges) { iter =>
      if (iter.hasNext) {
        val (start, end) = iter.next()
        Some((start to end).toIterator, iter)
      }
      else None
    }

    val read: Iterator[Long] => Process[Task, Vector[(K, V)]] = { iterator =>
      Process.unfold(iterator) { iter =>
        if (iter.hasNext) {
          val index = iter.next()
          val valueWritable = pathRanges
            .find { row => index >= row._2 && index <= row._3 }
            .map { case (p, _, _) => readers.getOrInsert(p, new MapFile.Reader(p, conf)) }
            .getOrElse(throw new LayerIOError(s"Index ${index} not found."))
            .get(new LongWritable(index), new BytesWritable())
            .asInstanceOf[BytesWritable]

          if (valueWritable == null) Some(Vector(), iter)
          else {
            val items = AvroEncoder.fromBinary(writerSchema.getOrElse(codec.schema), valueWritable.getBytes)(codec)
            if (indexFilterOnly) Some(items, iter)
            else Some(items.filter { row => includeKey(row._1) }, iter)
          }
        } else {
          None
        }
      }
    }

    val result =
      nondeterminism
        .njoin(maxOpen = threads, maxQueued = threads) { range map read }(Strategy.Executor(pool))
        .runFoldMap(identity).unsafePerformSync

    pool.shutdown(); result
  }
}

object HadoopCollectionReader {
  def apply(maxOpenFiles: Int = 16) = new HadoopCollectionReader(maxOpenFiles)
}
