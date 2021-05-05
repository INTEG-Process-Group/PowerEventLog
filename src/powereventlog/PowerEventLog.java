package powereventlog;

import com.integpg.system.Immutable;
import com.integpg.system.JANOS;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class PowerEventLog {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm:ss zzz");



    /**
     * log Uptime and Downtime of JNIOR to powerevents.log/powerevents.log.bak
     *
     * @param filename is the name of the file that is being logged to
     * @param bytes is the data in the form of a byte array being logged
     * @throws IOException
     */
    public static void appendAllBytes(String filename, byte[] bytes) throws IOException {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename, true);
            fos.write(bytes);
            fos.flush();
        } catch (Exception ex) {
            JANOS.syslog("appendAllBytes to " + filename + " threw " + ex.getMessage(), JANOS.SYSLOG_ERROR);
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                }
            } catch (IOException ex1) {
                ex1.printStackTrace();
            }
        }
    }



    /**
     * get or create Immutable array to keep track of time when JNIOR powers off
     *
     * @return
     */
    public static long[] getOrCreateImmutables() {

        long[] startAndLastUp = Immutable.getLongArray("StartandLast");

        if (Immutable.getLongArray("StartandLast") == null) {
            startAndLastUp = Immutable.createLongArray("StartandLast", 2);
        }

        return startAndLastUp;
    }



    /**
     * format string for how long JNIOR was running and how long it was off
     *
     * @param startedAndStopped is the immutable array storing the last boots
     * start and stop times
     * @param timeStarted is the time that the JNIOR booted up
     * @return
     */
    public static String formatOutput(long[] startedAndStopped, long timeStarted) {

        String dateString = SIMPLE_DATE_FORMAT.format(timeStarted);
        if (0 != startedAndStopped[1]) {
            long poweredUp = startedAndStopped[1] - startedAndStopped[0];
            int daysUp = (int) (poweredUp / 86400000);
            int hoursUp = (int) ((poweredUp % 86400000) / 3600000);
            int minutesUp = (int) (((poweredUp % 86400000) % 3600000) / 60000);
            int secondsUp = (int) ((((poweredUp % 86400000) % 3600000) % 60000) / 1000);

            long poweredDown = timeStarted - startedAndStopped[1];
            int daysDown = (int) (poweredDown / 86400000);
            int hoursDown = (int) ((poweredDown % 86400000) / 3600000);
            int minutesDown = (int) (((poweredDown % 86400000) % 3600000) / 60000);
            int secondsDown = (int) ((((poweredDown % 86400000) % 3600000) % 60000) / 1000);

            String output = String.format("%s, Up: %d days %02d:%02d:%02d. Down: %d days %02d:%02d:%02d\r\n",
                    dateString, daysUp, hoursUp, minutesUp, secondsUp, daysDown, hoursDown, minutesDown, secondsDown);
            return output;
        }

        return "First Time Running...\r\n";
    }



    /**
     * boolean check if backup exists
     *
     * @param valueToBeLogged is the data we are logging to the powerEvent files
     */
    public static void checkForBackup(String valueToBeLogged) {

        File powerEvents = new File("powerevents.log");
        File powerEventsBackup = new File("powerevents.log.bak");
        boolean backupExists = powerEventsBackup.exists();

        if ((powerEvents.length() + valueToBeLogged.getBytes().length) > 4096) {

            if (backupExists == false) {

                powerEvents.renameTo(powerEventsBackup);
            } else {

                powerEventsBackup.delete();
                powerEvents.renameTo(powerEventsBackup);
            }

        }
    }



    /**
     * checks if duplicate application is running, executes functions on boot,
     * stores current time in immutable array using a loop until JNIOR reboots
     *
     * @param args
     * @throws InterruptedException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {

        //check if duplicate process of PowerEventLog is running
        int runningprocesses = JANOS.registerProcess("PowerEventLog");
        boolean duplicate = 1 < runningprocesses;
        if (duplicate) {
            System.out.println("duplicate process");
            System.exit(0);
        }

        //log to Jniorsys.log
        JANOS.syslog("PowerEventLog v1.0");

        //gets value of when application started
        long startTime = System.currentTimeMillis() - JANOS.uptimeMillis();

        long[] startAndLastUp = getOrCreateImmutables();
        String infoToLog = formatOutput(startAndLastUp, startTime);
        checkForBackup(infoToLog);
        appendAllBytes("powerevents.log", infoToLog.getBytes());

        //update start time in Immutable
        startAndLastUp[0] = startTime;

        //loop
        while (true) {

            //    update last runtime
            startAndLastUp[1] = System.currentTimeMillis();
            //    sleep for 1 second
            Thread.sleep(1000);

        }

    }

}
