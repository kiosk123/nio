package ch08.socket;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author Apress
 */
public class NioClient {

    public static void main(String[] args) {

        final int DEFAULT_PORT = 5555;
        final String IP = "127.0.0.1";

        ByteBuffer buffer = ByteBuffer.allocateDirect(2 * 1024);
        ByteBuffer randomBuffer;
        CharBuffer charBuffer;

        Charset charset = Charset.defaultCharset();
        CharsetDecoder decoder = charset.newDecoder();

        // open() 메서드를 사용하여 Selector와 ServerSocketChannel을 연다
        try (Selector selector = Selector.open();
                SocketChannel socketChannel = SocketChannel.open()) {

            // check that both of them were successfully opened
            if ((socketChannel.isOpen()) && (selector.isOpen())) {

                // configure non-blocking mode
                socketChannel.configureBlocking(false);

                // set some options
                socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 128 * 1024);
                socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 128 * 1024);
                socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

                // register the current channel with the given selector - OP_CONNECT 클라이언트에서 사용
                socketChannel.register(selector, SelectionKey.OP_CONNECT);

                // connect to remote host
                socketChannel.connect(new java.net.InetSocketAddress(IP, DEFAULT_PORT));

                System.out.println("Localhost: " + socketChannel.getLocalAddress());

                // waiting for the connection
                while (selector.select(1000) > 0) {

                    // get keys
                    Set keys = selector.selectedKeys();
                    Iterator its = keys.iterator();

                    // process each key
                    while (its.hasNext()) {
                        SelectionKey key = (SelectionKey) its.next();

                        // remove the current key
                        its.remove();

                        // get the socket channel for this key
                        try (SocketChannel keySocketChannel = (SocketChannel) key.channel()) {

                            // attempt a connection
                            if (key.isConnectable()) {

                                // signal connection success
                                System.out.println("I am connected!");

                                // close pendent connections
                                if (keySocketChannel.isConnectionPending()) {
                                    keySocketChannel.finishConnect();
                                }

                                // read/write from/to server
                                while (keySocketChannel.read(buffer) != -1) {

                                    buffer.flip();

                                    charBuffer = decoder.decode(buffer);
                                    System.out.println(charBuffer.toString());

                                    if (buffer.hasRemaining()) {
                                        buffer.compact();
                                    } else {
                                        buffer.clear();
                                    }

                                    int r = new Random().nextInt(100);
                                    if (r == 50) {
                                        System.out.println("50 was generated! Close the socket channel!");
                                        break;
                                    } else {
                                        randomBuffer = ByteBuffer
                                                .wrap("Random number:".concat(String.valueOf(r)).getBytes("UTF-8"));
                                        keySocketChannel.write(randomBuffer);
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            System.err.println(ex);
                        }
                    }
                }
            } else {
                System.out.println("The socket channel or selector cannot be opened!");
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }

    }
}
