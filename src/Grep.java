import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Search web pages for lines containing a string. */
public class Grep {
    public static void main(String[] args) throws Exception {
        
        // substring to search for
        String substring = "specification";
        
        // URLs to search
        String[] urls = new String[] {
                "http://web.mit.edu/6.005/www/fa14/psets/ps0/",
                "http://web.mit.edu/6.005/www/fa14/psets/ps1/",
                "http://web.mit.edu/6.005/www/fa14/psets/ps2/",
                "http://web.mit.edu/6.005/www/fa14/psets/ps3/",
        };
        
        // list for accumulating matching lines
        List<Item> matches = Collections.synchronizedList(new ArrayList<>());
        
        // queue for sending lines from producers to consumers
        BlockingQueue<Item> queue = new LinkedBlockingQueue<Item>();
        
        Thread[] producers = new Thread[urls.length]; // one producer per URL
        Thread[] consumers = new Thread[2]; // multiple consumers
        
        for (int ii = 0; ii < consumers.length; ii++) { // start Consumers
            Thread consumer = consumers[ii] = new Thread(new Consumer(substring, queue, matches));
            consumer.start();
        }
        
        for (int ii = 0; ii < urls.length; ii++) { // start Producers
            Thread producer = producers[ii] = new Thread(new Producer(urls[ii], queue));
            producer.start();
        }
        
        for (Thread producer : producers) { // wait for Producers to stop
            producer.join();
        }
        
        for (Thread consumer : consumers) { // stop all the consumers
            queue.add(new Stop());
        }
       
        for (Thread consumer : consumers) { // wait for Consumers to stop
            consumer.join();
        }
        
        for (Item match : matches) {
            System.out.println(match);
        }
        System.out.println(matches.size() + " lines matched");
    }
}

class Producer implements Runnable {
    
    private final String url;
    private final BlockingQueue<Item> queue;
    
    Producer(String url, BlockingQueue<Item> queue) {
        this.url = url;
        this.queue = queue;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            int lineNumber = 0;
            String line;
            while ((line = in.readLine()) != null) {
                queue.add(new Text(url, lineNumber++, line));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

class Consumer implements Runnable {
    
    private final String substring;
    private final BlockingQueue<Item> queue;
    private final List<Item> matches;
    
    Consumer(String substring, BlockingQueue<Item> queue, List<Item> matches) {
        this.substring = substring;
        this.queue = queue;
        this.matches = matches;
    }

    public void run() {
        try {
            while (true) {
                Item line = queue.take();
                if (line.isEnd()) {
                    break;
                }
                if (line.text().contains(substring)) {
                    matches.add(line);
                }
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}


/** Represents single item of work. */
interface Item {
    /** @return the filename, requires !isEnd */
    public String filename();
    /** @return the line number, requires !isEnd */
    public int lineNumber();
    /** @return the text, requires !isEnd */
    public String text();
    /** @return true if there's no more work to do */
    public boolean isEnd();
}

/** Represents a line of text. */
class Text implements Item {
    private final String filename;
    private final int lineNumber;
    private final String text;
    
    public Text(String filename, int lineNumber, String text) {
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.text = text;
    }
    
    public String filename() {
        return filename;
    }
    
    public int lineNumber() {
        return lineNumber;
    }
    
    public String text() {
        return text;
    }
    
    public boolean isEnd() {
        return false;
    }
    
    @Override public String toString() {
        return filename + ":" + lineNumber + " " + text;
    }
}

/** Represents a request to stop processing. */
class Stop implements Item {
    
    public String filename() {
        throw new UnsupportedOperationException();
    }
    
    public int lineNumber() {
        throw new UnsupportedOperationException();
    }
    
    public String text() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isEnd() {
        return true;
    }
    
    @Override public String toString() {
        return "STOP";
    }
}
