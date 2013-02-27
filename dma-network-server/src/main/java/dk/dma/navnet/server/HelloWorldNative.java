package dk.dma.navnet.server;

public class HelloWorldNative {
    public static String hello(String name) {
        return "Hello, " + name;
    }

    public static void main(String args[]) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            // hello("world" + i);
            System.out.println(hello("world" + i));
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start + " ms");
    }
}
