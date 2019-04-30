package abzu.ast.builtin;

import abzu.AbzuException;
import abzu.runtime.NativeObject;
import abzu.runtime.Tuple;
import abzu.runtime.async.AbzuFuture;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

@NodeInfo(shortName = "freadline")
public abstract class FileReadLineNode extends BuiltinNode {
  @Specialization
  public AbzuFuture freadline(Tuple fileTuple) {
    AsynchronousFileChannel asynchronousFileChannel = (AsynchronousFileChannel) ((NativeObject) fileTuple.get(0)).getValue();
    ByteBuffer buffer = ByteBuffer.allocate(1024 * 10);
    long position = (long) fileTuple.get(2);
    Node thisNode = this;
//    Promise promise = new Promise("freadline_" + position);
    AbzuFuture future = new AbzuFuture();

    asynchronousFileChannel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
      @Override
      public void completed(Integer result, ByteBuffer attachment) {
        boolean fulfilled = false;
        if (result <= 0) {
//          promise.fulfil("eof");
          future.completableFuture.complete("eof");
          return;
        }

        attachment.flip();
        int length = attachment.limit();
        StringBuffer output = (StringBuffer) fileTuple.get(1);

        if (output == null) output = new StringBuffer();

        for (int i = 0; i < length; i++) {
          char ch = ((char) attachment.get());
          if (ch == '\n') {
//            promise.fulfil(new Tuple("ok", output.toString(), new Tuple(new NativeObject(asynchronousFileChannel), null, position + i + 1)));
            future.completableFuture.complete(new Tuple("ok", output.toString(), new Tuple(new NativeObject(asynchronousFileChannel), null, position + i + 1)));
            fulfilled = true;
            break;
          } else {
            output.append(ch);
          }
        }
        attachment.clear();

        if (!fulfilled) {
          freadline(new Tuple(new NativeObject(asynchronousFileChannel), output, position + length)).completableFuture.thenApply(res -> {
//            promise.fulfil(res);
            future.completableFuture.complete(res);
            return res;
          });
//          promise.fulfil(new Tuple("ok", "test", new Tuple(new NativeObject(asynchronousFileChannel), null, position + length + 1)));
        }
      }

      @Override
      public void failed(Throwable exc, ByteBuffer attachment) {
//        promise.fulfil(new AbzuException(exc.getMessage(), thisNode));
        future.completableFuture.completeExceptionally(new AbzuException(exc.getMessage(), thisNode));
      }
    });

//    return promise;
    return future;
  }
}
