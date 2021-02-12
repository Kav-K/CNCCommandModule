import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;

public class Lock {
    public static final String USER_HOME = System.getProperty("user.home");
    public static final File LOCK_FILE = new File(USER_HOME, "cncclient.lock");
    private FileLock lock;


    public Lock()  {
        startLock();


    }
    public FileLock getLock() {
        return lock;
    }

    private void startLock()  {

           try {
               FileChannel fc = FileChannel.open(LOCK_FILE.toPath(),
                       StandardOpenOption.CREATE,
                       StandardOpenOption.WRITE);
               lock = fc.tryLock();

           } catch (IOException e) {
               Main.error(e.toString());

           }


    }







}
