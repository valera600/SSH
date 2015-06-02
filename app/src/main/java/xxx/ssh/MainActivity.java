package xxx.ssh;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.maverick.ssh.ChannelOpenException;
import com.maverick.ssh.SshAuthentication;
import com.maverick.ssh.SshClient;
import com.maverick.ssh.SshClientConnector;
import com.maverick.ssh.SshConnector;
import com.maverick.ssh.SshException;
import com.maverick.ssh.SshSession;
import com.sshtools.net.SocketTransport;

import java.io.IOException;
//import java.util.Timer;
//import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SshClient ssh = null;
    private String serverIp = "192.168.43.213";
    private boolean up = false;
    private boolean left = false;
    private boolean down = false;
    private boolean right = false;
    private boolean lastUp = false;
    private boolean lastDown = false;
    private boolean lastLeft = false;
    private boolean lastRight = false;
    private boolean manualControl = true;
    private boolean wasChanged = false;
    private TextView tvInfo;
    private Button btnUp = null;
    private Button btnLeft = null;
    private Button btnDown = null;
    private Button btnRight = null;
    private SensorManager msensorManager = null; //Менеджер сенсоров аппрата
    private int borderAngle = 15;
    private int anglePWM = 0;
    private boolean changedSpeed = false;

    //private Timer timer;
    //private MyTimerTask timerTask;

    private final String myErrorLogTag = "my error";

    private final int timerPeriod = 500;

    private final int GPIOPortUp = 2;
    private final int GPIOPortDown = 4;
    private final int GPIOPortLeft = 3;
    private final int GPIOPortRight = 17;

    private float[] rotationMatrix;     //Матрица поворота
    private float[] accelData;           //Данные с акселерометра
    private float[] magnetData;       //Данные геомагнитного датчика
    private float[] OrientationData; //Матрица положения в пространстве

    private TextView xzView;
    private TextView zyView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvInfo = (TextView) findViewById(R.id.textView);
        btnUp = (Button) findViewById(R.id.buttonUp);
        btnDown = (Button) findViewById(R.id.buttonDown);
        btnLeft = (Button) findViewById(R.id.buttonLeft);
        btnRight = (Button) findViewById(R.id.buttonRight);
        Button btnTypeControl = (Button) findViewById(R.id.buttonChangeTypeControl);
        Button btnConnect = (Button) findViewById(R.id.buttonConnect);
        Button btnDisconnect = (Button) findViewById(R.id.buttonDisconnect);
        //timer = new Timer();
        //timerTask = new MyTimerTask();

        msensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        rotationMatrix = new float[16];
        accelData = new float[3];
        magnetData = new float[3];
        OrientationData = new float[3];

        //листенер для смены управления
        final View.OnClickListener oclTypeControl = new View.OnClickListener() {
            public void onClick(View v){
                try {
                    if (manualControl) {
                        manualControl = false;
                        turnOnSensor();
                        stopMotion();
                    } else {
                        btnUp.setText(R.string.buttonUp);
                        btnLeft.setText(R.string.buttonLeft);
                        btnDown.setText(R.string.buttonDown);
                        btnRight.setText(R.string.buttonRight);
                        btnUp.setTextColor(Color.BLACK);
                        btnLeft.setTextColor(Color.BLACK);
                        btnRight.setTextColor(Color.BLACK);
                        btnDown.setTextColor(Color.BLACK);
                        turnOffSensor();
                        manualControl = true;
                        stopMotion();
                    }
                }
                catch (Throwable t)
                {
                    Log.d(myErrorLogTag, t.toString());
                }
            }
        };

        //листенер для ручного управления
        View.OnTouchListener oclBtn = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                try {
                    switch (v.getId()) {
                        //up
                        case R.id.buttonUp:
                            switch (motionEvent.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    tvInfo.setText("1 2");
                                    if (!down)
                                        up = true;
                                    break;
                                case MotionEvent.ACTION_UP:
                                case MotionEvent.ACTION_CANCEL:
                                    tvInfo.setText("0 2");
                                    up = false;
                                    break;
                            }
                            break;
                        //left
                        case R.id.buttonLeft:
                            switch (motionEvent.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    tvInfo.setText("1 3");
                                    if (!right)
                                        left = true;
                                    break;
                                case MotionEvent.ACTION_UP:
                                case MotionEvent.ACTION_CANCEL:
                                    tvInfo.setText("0 3");
                                    left = false;
                                    break;
                            }
                            break;
                        //down
                        case R.id.buttonDown:
                            switch (motionEvent.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    tvInfo.setText("1 4");
                                    if (!up)
                                        down = true;
                                    break;
                                case MotionEvent.ACTION_UP:
                                case MotionEvent.ACTION_CANCEL:
                                    tvInfo.setText("0 4");
                                    down = false;
                                    break;
                            }
                            break;
                        //right
                        case R.id.buttonRight:
                            switch (motionEvent.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    tvInfo.setText("1 17");
                                    if (!left)
                                        right = true;
                                    break;
                                case MotionEvent.ACTION_UP:
                                case MotionEvent.ACTION_CANCEL:
                                    tvInfo.setText("0 17");
                                    right = false;
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                    updateMotion();
                } catch (Throwable t) {
                    Log.d(myErrorLogTag, t.toString());
                    return false;
                }
                return true;
            }
        };

        View.OnClickListener oclDisconnect = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    stopMotion();
                    tvInfo.setText("Disconnected!");
                    if(!manualControl)
                        oclTypeControl.onClick(v);
                    //timer.cancel();
                    ssh.disconnect();
                }
                catch (Throwable t)
                {
                    Log.d(myErrorLogTag, t.toString());
                }
            }
        };


        View.OnClickListener oclConnect = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyTask mt = new MyTask();
                mt.execute();
                //timer.schedule(timerTask,0,timerPeriod);
            }
        };

        btnUp.setOnTouchListener(oclBtn);
        btnLeft.setOnTouchListener(oclBtn);
        btnDown.setOnTouchListener(oclBtn);
        btnRight.setOnTouchListener(oclBtn);

        btnTypeControl.setOnClickListener(oclTypeControl);
        btnConnect.setOnClickListener(oclConnect);
        btnDisconnect.setOnClickListener(oclDisconnect);

        com.maverick.ssh.LicenseManager.addLicense("----BEGIN 3SP LICENSE----\r\n"
                + "Product : J2SSH Maverick\r\n"
                + "Licensee: home\r\n"
                + "Comments: valera\r\n"
                + "Type    : Evaluation License\r\n"
                + "Created : 20-May-2015\r\n"
                + "Expires : 04-Jul-2015\r\n"
                + "\r\n"
                + "378720442AD7AFDBD9FF0D7E356DDB3726C8F941F73461DB\r\n"
                + "9EA464FFDE8ED058DD243D641C0CA39A59C34D5F39DBF626\r\n"
                + "4A6B7861CE00DE15D79180416149437F0466391277AD9C5F\r\n"
                + "A6B99159B3B624F119190AE0EB84F693A7BEA5C942D96DA2\r\n"
                + "3B14B770034FFF022CFB302939A700B04348B624CF1D741C\r\n"
                + "6245D5119B975A2778590441933FA164275270BAB973A93D\r\n"
                + "----END 3SP LICENSE----\r\n");


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            updateMotion();
        }
    }*/

    //connection
    private class MyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            try {
                int port = 22;

                String username = "pi";

                /**
                 * Create an SshConnector instance
                 */
                SshClientConnector con = SshConnector.createInstance();

                /**
                 * Connect to the host
                 */
                ssh = con.connect(new SocketTransport(serverIp,
                        port), username, true);

                /**
                 * Authenticate the user using password authentication
                 */

                com.maverick.ssh.PasswordAuthentication pwd = new com.maverick.ssh.PasswordAuthentication();

                do {
                    System.out.print("Password: raspberry");
                    pwd.setPassword("raspberry");
                } while (ssh.authenticate(pwd) != SshAuthentication.COMPLETE
                        && ssh.isConnected());

                /**
                 * Start a session and do basic IO
                 */
                if (ssh.isAuthenticated()) {

                    /**
                     * Create a Shell*/

                    SshSession session = ssh.openSessionChannel();
                    session.executeCommand("ls");
                    session.close();
                }
                //ssh.disconnect();

            } catch (SshException | IOException | ChannelOpenException e) {
                Log.d(myErrorLogTag, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            tvInfo.setText("Connected!");
            Toast toast = Toast.makeText(getApplicationContext(),
                    "We are connected with Pi!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private class ThreadSSH implements Runnable {

        private String cmd;

        public void run() {
            updateGPIO updGPIO = new updateGPIO();
            updGPIO.cmd = cmd;
            Thread thr = new Thread(updGPIO);
            thr.start();
        }
    }

    private class updateGPIO extends Thread implements Runnable {

        public String cmd;

        public void run(){
            if(ssh.isAuthenticated()) {
                try {
                    SshSession session = ssh.openSessionChannel();
                    session.executeCommand(cmd);
                    session.close();
                } catch (SshException | ChannelOpenException e) {
                    Log.d(myErrorLogTag,e.toString());
                }
            }
            this.interrupt();
        }

    }

    private void updateMotion()
    {
        try {
            if(ssh.isAuthenticated()) {
                ThreadSSH thrssh[] = new ThreadSSH[4];
                Thread thr[] = new Thread[4];
                String str;
                for(int i = 0; i < 4; i++)
                    thrssh[i] = new ThreadSSH();

                if(manualControl) {
                    //up
                    if(up != lastUp)
                    {
                        str = "echo " + Integer.toString(up ? 1 : 0) + " > /sys/class/gpio/gpio" + Integer.toString(GPIOPortUp) + "/value";
                        thrssh[0].cmd = str;
                        thr[0] = new Thread(thrssh[0]);
                        thr[0].start();
                        lastUp = up;
                    }

                    //down
                    if(down != lastDown) {
                        str = "echo " + Integer.toString(down ? 1 : 0) + " > /sys/class/gpio/gpio" + Integer.toString(GPIOPortDown) + "/value";
                        thrssh[1].cmd = str;
                        thr[1] = new Thread(thrssh[1]);
                        thr[1].start();
                        lastDown = down;
                    }
                }

                //left
                if(left != lastLeft) {
                    str = "echo " + Integer.toString(left ? 1 : 0) + " > /sys/class/gpio/gpio" + Integer.toString(GPIOPortLeft) + "/value";
                    thrssh[2].cmd = str;
                    thr[2] = new Thread(thrssh[2]);
                    thr[2].start();
                    lastLeft = left;
                }

                //right
                if(right != lastRight) {
                    str = "echo " + Integer.toString(right ? 1 : 0) + " > /sys/class/gpio/gpio" + Integer.toString(GPIOPortRight) + "/value";
                    thrssh[3].cmd = str;
                    thr[3] = new Thread(thrssh[3]);
                    thr[3].start();
                    lastRight = right;
                }
            }
        }catch (Throwable t)
        {
            Log.d(myErrorLogTag, t.toString());
        }
    }

    //orientation
    @Override
    protected void onResume() {
        super.onResume();
        turnOnSensor();
    }


    //Использовать включение и выключение сенсоров, когда не ручной режим управления
    private void turnOnSensor()
    {
        if (!manualControl) {
            msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
            msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void turnOffSensor()
    {
        if(!manualControl)
            msensorManager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        turnOffSensor();
    }

    private void loadNewSensorData(SensorEvent event) {
        final int type = event.sensor.getType(); //Определяем тип датчика
        if (type == Sensor.TYPE_ACCELEROMETER) { //Если акселерометр
            accelData = event.values.clone();
        }

        if (type == Sensor.TYPE_MAGNETIC_FIELD) { //Если геомагнитный датчик
            magnetData = event.values.clone();
        }
    }

    public void onSensorChanged(SensorEvent event) {
            try {
                loadNewSensorData(event); // Получаем данные с датчика
                SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magnetData); //Получаем матрицу поворота
                SensorManager.getOrientation(rotationMatrix, OrientationData); //Получаем данные ориентации устройства в пространстве

                //Выводим результат
                int y = (int) Math.round(Math.toDegrees(OrientationData[1]));
                int z = (int) Math.round(Math.toDegrees(OrientationData[2]));
                if(Math.abs(Math.abs(y) - anglePWM) > 2) { //исключаем дрожание рук
                    changedSpeed = true;    //скорость была изменена
                    anglePWM = Math.abs(y);  //угол для ШИМ
                }
                btnUp.setText(Integer.toString(y));
                btnDown.setText(Integer.toString(0 - y));
                btnRight.setText(Integer.toString(z));
                btnLeft.setText(Integer.toString(0 - z));

                if(changedSpeed) {
                    changedSpeed = false;
                    String str;
                    ThreadSSH thrssh = new ThreadSSH();
                    //up
                    if (y > 5) {
                        down = false;
                        up = true;
                        str = "echo " + Integer.toString(GPIOPortUp) + " > /home/pi/onPWM";
                        thrssh.cmd = str;
                        Thread thr = new Thread(thrssh);
                        thr.start();
                        btnUp.setTextColor(Color.GREEN);
                        btnDown.setTextColor(Color.RED);

                    }

                    //down
                    if (y < -5) {
                        up = false;
                        down = true;
                        str = "echo " + Integer.toString(GPIOPortDown) + " > /home/pi/onPWM";
                        thrssh.cmd = str;
                        Thread thr = new Thread(thrssh);
                        thr.start();
                        btnUp.setTextColor(Color.RED);
                        btnDown.setTextColor(Color.GREEN);
                    }

                    //non up and non down
                    if (y >= -5 && y <= 5) {
                        up = false;
                        down = false;
                        str = "echo 0 > /home/pi/onPWM";
                        thrssh.cmd = str;
                        Thread thr = new Thread(thrssh);
                        thr.start();
                        btnUp.setTextColor(Color.BLACK);
                        btnDown.setTextColor(Color.BLACK);
                    }

                    if(up || down)
                    {
                        ThreadSSH thrsshAngle = new ThreadSSH();
                        thrsshAngle.cmd = "echo " + Integer.toString(anglePWM*4) + " > /home/pi/delay";
                        Thread thr = new Thread(thrsshAngle);
                        thr.start();
                    }
                }

                //right
                if (z >= borderAngle) {
                    if (!right) {
                        wasChanged = true;
                        right = true;
                        left = false;
                        btnLeft.setTextColor(Color.RED);
                        btnRight.setTextColor(Color.GREEN);
                    }
                }
                //left
                if (z <= -borderAngle) {
                    if (!left) {
                        wasChanged = true;
                        left = true;
                        right = false;
                        btnLeft.setTextColor(Color.GREEN);
                        btnRight.setTextColor(Color.RED);
                    }
                }
                //non right and non left
                if (z > -borderAngle && z < borderAngle) {
                    if (left || right) {
                        wasChanged = true;
                        left = false;
                        right = false;
                        btnLeft.setTextColor(Color.BLACK);
                        btnRight.setTextColor(Color.BLACK);
                    }
                }
                //apply changes for left/right
                if (wasChanged) {
                    wasChanged = false;
                    updateMotion();
                }
            } catch (Throwable t) {
                Log.d(myErrorLogTag, t.toString());
            }
    }

    private void stopMotion()
    {
        up = false;
        down = false;
        left = false;
        right = false;
        try {
            updateMotion();
        }
        catch (Throwable t)
        {
            Log.d(myErrorLogTag, t.toString());
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { //Изменение точности показаний датчика
    }
}
