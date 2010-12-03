import java.io.*;
import java.net.*;

public class wget
{
  public static void main(String[] args)
  {
    if (args.length < 1) {
      System.err.println("Usage: java wget <URL>");
      System.exit(1);
    }

    String url = args[0];

    URL u;
    InputStream is = null;
    DataInputStream dis = null;
    int c;

    try {
      u = new URL(url);
      is = u.openStream();
      dis = new DataInputStream(new BufferedInputStream(is));
      while ((c = dis.read()) != -1) {
        System.out.write(c);
      }
    }
    catch (MalformedURLException mue) {
      System.err.println("ERROR: Malformed URL specified");
      mue.printStackTrace();
      System.exit(2);
    }
    catch (IOException ioe) {
      System.err.println("ERROR: I/O Exception occured");
      ioe.printStackTrace();
      System.exit(3);
    }
    finally {
      try {
        is.close();
        System.out.close();
      }
      catch (IOException ioe) {
      }
    }
  }
}
