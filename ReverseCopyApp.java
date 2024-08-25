import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.*;

/**
 * The ReversedCopyApp class is designed to copy the contents of one file to another,
 * reversing the order of the bytes. It uses {@link FileChannel} to read and write files
 * with the option to specify the buffer size.
 */
public class ReversedCopyApp {
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * The main method of the program. It processes command-line arguments and initiates
     * the file copying process with reversed byte order.
     *
     * @param args command-line arguments:
     *             <ul>
     *                 <li><code>&lt;src&gt;</code> - the path to the source file</li>
     *                 <li><code>&lt;dst&gt;</code> - the path to the destination file</li>
     *                 <li><code>-b</code> or <code>--buffer</code> <code>&lt;buffer size&gt;</code> (optional)
     *                 - the buffer size in bytes (default: 1024)</li>
     *                 <li><code>-h</code> or <code>--help</code> - displays help information</li>
     *             </ul>
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length == 3 || args.length > 4) {
            System.err.println("Invalid number of arguments.");
            System.exit(1);
        }

        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println("""
                    Usage: java ReversedCopyApp <src> <dst> [-b <buffer size>]
                      -h, --help     Show help message
                      -b, --buffer   Specify buffer size in bytes (default: 1024)
                      <src>          Source file path
                      <dst>          Destination file path
                    """);
            System.exit(0);
        }

        String src = args[0];
        String dst = args[1];
        int bufferSize = DEFAULT_BUFFER_SIZE;

        if (args.length == 4) {
            if (args[2].equals("-b") || args[2].equals("--buffer")) {
                try {
                    bufferSize = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid buffer size.");
                    System.exit(1);
                }
            } else {
                System.err.println("Unknown option: " + args[2]);
                System.exit(1);
            }
        }

        try {
            copyReversed(src, dst, bufferSize);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Copies the content of the source file to the destination file in reverse byte order.
     *
     * @param src        the path to the source file
     * @param dst        the path to the destination file
     * @param bufferSize the buffer size for reading/writing in bytes
     * @throws IllegalArgumentException if the file paths are empty or the buffer size is less than 1
     * @throws RuntimeException         if the file is not found or an I/O error occurs
     */
    public static void copyReversed(String src, String dst, int bufferSize) {
        if (src.isBlank() || dst.isBlank() || bufferSize < 1) {
            throw new IllegalArgumentException("Paths to source and destination files must not be blank" +
                    "and buffer size must be greater than 0.");
        }

        try (FileChannel channelToSrc = FileChannel.open(Path.of(src), READ);
             FileChannel channelToDst = FileChannel.open(Path.of(dst), WRITE, CREATE, TRUNCATE_EXISTING)) {

            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            long position = channelToSrc.size();

            while (position > 0) {
                long bytesToRead = Math.min(bufferSize, position);
                position -= bytesToRead;
                channelToSrc.position(position);

                buffer.clear();
                channelToSrc.read(buffer);

                buffer.flip();
                reverseBuffer(buffer);
                channelToDst.write(buffer);
            }
        } catch (NoSuchFileException e) {
            throw new RuntimeException("No such source file exists.");
        } catch (IOException e) {
            throw new RuntimeException("Unexpected I/O error occurred.");
        }
    }

    /**
     * Reverses the contents of the given {@link ByteBuffer}.
     *
     * @param buffer the buffer to be reversed
     */
    private static void reverseBuffer(ByteBuffer buffer) {
        int i = 0;
        int j = buffer.limit() - 1;

        while (i < j) {
            byte tmp = buffer.get(i);
            buffer.put(i, buffer.get(j));
            buffer.put(j, tmp);
            i++;
            j--;
        }
    }
}

