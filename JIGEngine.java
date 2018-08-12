import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;
import java.lang.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import javax.imageio.*;

public class JIGEngine extends JPanel implements Runnable, KeyListener, MouseInputListener
{
    boolean DEV_MODE;
    boolean CONSOLE_ON;
    boolean WIREFRAMES;
    boolean LABEL_DRAWORDER;
    boolean ANTIALIASING_ON;
    
    boolean paused;
    boolean showconsole;
        
    Thread main;
    Graphics2D dbg;
    Image img;
    
    String consoleinput;
    String[] consolelog;
    String lastconsoleinput;
    
    Robot bot;
    int mxc, myc;
    
    double tilesize;
    double radius, theta, vtheta, hfactor, yscale, dy, dx, cy, cx, rotatespeed, zoomspeed, movespeed, moveinc, spacex, spacey;
    double targetcx, targetcy, bht;
    int consolescrollup;
    int ctx, cty;
    double targettheta, targetyscale, targetzoom;
    boolean sliding = false;  boolean homing = false;
    boolean rotatingleft, rotatingright, rotatingup, rotatingdown, zoomingin, zoomingout, movingW, movingA, movingS, movingD;
    double dxT, dyT;
    int tileswide, tileslong;
    int[][] draworder;
    int draworientation;
    
    // temporary vars!
    double redcubeAcc;
    double redcubeVel;
    // end temp vars
    
    JIGCell[][] board;
    
    public void run()
    {
        img = createImage(getSize().width, getSize().height);
        dbg = (Graphics2D)img.getGraphics();
        if(ANTIALIASING_ON) dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        
        while(true)
        {
            if(!paused) act();
            drawToImage();
            if(paused) drawPaused();
            drawToScreen();
        }
    }
    
    public void drawCells()
    {
        boolean donedrawing = false;
        int layer = 0;
        while(!donedrawing)
        {
            donedrawing = true;
            for(int t=0; t<draworder.length; t++)
            {
                int tx = draworder[t][0];
                int ty = draworder[t][1];
                JIGCell c = board[tx][ty];
                if(layer < c.platforms.length)
                {
                    drawPlatform(c.platforms[layer],tx,ty);
                    donedrawing = false;
                }
            }
            layer++;
        }
    }
    
    public void home(double x, double y)
    {
        home(x,y,theta,yscale,tilesize);
    }
    
    public void home(double x, double y, double th)
    {
        home(x,y,th,yscale,tilesize);
    }
    
    public void home(double x, double y, double th, double ys)
    {
        home(x,y,th,ys,tilesize);
    }
    
    public void home(double x, double y, double th, double ys, double ts)
    {
        slide(x,y,th,ys,ts);
        homing = true;
    }
    
    public void stophoming()
    {
        homing = false;
        stopsliding();
    }
    
    public void slide(double x, double y)
    {
        slide(x,y,theta,yscale,tilesize);
    }
    
    public void slide(double x, double y, double th)
    {
        slide(x,y,th,yscale,tilesize);
    }
    
    public void slide(double x, double y, double th, double ys)
    {
        slide(x,y,th,ys,tilesize);
    }
    
    public void slide(double x, double y, double th, double ys, double ts)
    {
        sliding = true;
        targettheta = th;
        if(Math.abs(targettheta-theta) > Math.abs((targettheta+(2*Math.PI))-theta))
        {
            targettheta += (2*Math.PI);
        }
        targetyscale = ys;
        targetzoom = ts;
        goToTile(x,y);
    }
    
    public void stopsliding()
    {
        sliding = false;
        targettheta = theta;
        targetyscale = yscale;
    }
    
    public void drawPlatform(JIGPlatform p, int tx, int ty)
    {
        if(p.shown)
        {
            if(!p.textured)
            {
                int[][] corners = corners(tx,ty,p.posDX,p.posDY,p.rotDT);
                int c0x = corners[0][0];
                int c0y = corners[0][1];
                int c1x = corners[1][0];
                int c1y = corners[1][1];
                int c2x = corners[2][0];
                int c2y = corners[2][1];
                int c3x = corners[3][0];
                int c3y = corners[3][1];
                //double hscale = (1-yscale);  if(hscale > 0.7) hscale = 0.7;
                //if(hfactor > 0.7) hfactor = 0.7;
                double hmult = (4*Math.PI/9);
                int bh = (int)((p.baseheight)*(hfactor)*(radius*hmult));
                int hf = (int)(((p.baseheight)*(hfactor)*(radius*hmult)) + ((p.height)*(hfactor)*(radius*hmult)));
                int[] topX = new int[]{ c0x, c1x, c2x, c3x };
                int[] topY = new int[]{ c0y-hf, c1y-hf, c2y-hf, c3y-hf };
                int[] sd1X = new int[]{ c0x, c1x, c1x, c0x };
                int[] sd1Y = new int[]{ c0y-bh, c1y-bh, c1y-hf, c0y-hf };
                int[] sd2X = new int[]{ c0x, c3x, c3x, c0x };
                int[] sd2Y = new int[]{ c0y-bh, c3y-bh, c3y-hf, c0y-hf };
                if(!WIREFRAMES)
                {
                        dbg.setColor(p.fillcolor);
                        dbg.fillPolygon(topX,topY,4);
                        dbg.fillPolygon(sd1X,sd1Y,4);
                        dbg.fillPolygon(sd2X,sd2Y,4);
                }
                if(!WIREFRAMES) dbg.setColor(p.outlinecolor);
                else dbg.setColor(Color.green);
                dbg.drawPolygon(topX,topY,4);
                dbg.drawPolygon(sd1X,sd1Y,4);
                dbg.drawPolygon(sd2X,sd2Y,4);
            }
        }
    }
    
    public void goToTile(double tx, double ty)
    {
        goToTile((int)tx,(int)ty);   
    }
    
    public void goToTile(int tx, int ty)
    {
        if(tx >= 0 && tx < tileswide && ty >= 0 && ty < tileslong)
        {
            double targetcx = cx;
            double targetcy = cy;
            cx = getTilePosition(tx,ty)[0];
            cy = getTilePosition(tx,ty)[1];
            ctx = tx;
            cty = ty;
        }
    }
    
    public JIGEngine()
    {
        DEV_MODE = true;
        CONSOLE_ON = true;
        WIREFRAMES = false;
        LABEL_DRAWORDER = false;
        ANTIALIASING_ON = false;
        
        consolescrollup = 0;
        consoleinput = "";
        lastconsoleinput = "";
        clearconsole();
        
        paused = false;
        showconsole = false;    
        
        redcubeVel = 0.06;
        redcubeAcc = 0.003;
        
        try { bot = new Robot(); } catch(Exception e) {}
        
        rotatingleft = false; rotatingright = false;
        rotatingup = false; rotatingdown = false;
        zoomingin = false; zoomingout = false;
        
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension scrDim = tk.getScreenSize();
        
        setPreferredSize(scrDim);
        
        this.setFocusable(true);
        
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        
        requestFocusInWindow();
        
        main = new Thread(this);
        
        tilesize = 800.0;
        tileswide = 15;
        tileslong = 15;
        board = new JIGCell[tileswide][tileslong];
        for(int x=0; x<tileswide; x++)
            for(int y=0; y<tileslong; y++)
            {
                board[x][y] = new JIGCell();
            }
        // ADD CUBES HERE!
        board[4][4].addPlatform(new JIGPlatform(0.0,1.0,Color.black,Color.blue));
        board[4][5].addPlatform(new JIGPlatform(0.0,1.0,Color.black,Color.blue));
        board[4][5].addPlatform(new JIGPlatform(1.0,1.0,Color.black,Color.blue));
        board[5][5].addPlatform(new JIGPlatform(0.0,1.0,Color.black,Color.blue));
        board[10][8].addEmptyPlatform();
        board[10][8].addPlatform(new JIGPlatform(1.5,1.0,Color.black,Color.red));
        board[8][11].addPlatform(new JIGPlatform(0.0,1.0,Color.black,Color.green));
        // DONE ADDING CUBES
        ctx = 7;  cty = 7;
        radius = Math.sqrt(2*((int)tilesize^2));
        rotatespeed = 0.05;
        zoomspeed = 30.0;
        movespeed = 0.25;
        theta = 0.0; // CHANGES WITH ROTATION
        yscale = 1.0; // CHANGES WITH CAMERA ANGLE
        
        cy = -1;
        cx = -1;
        targetcy = -1;
        targetcx = -1;
    }
    
    public void pause()
    {
            paused = true;
    }
    
    public void unpause()
    {
            paused = false;
    }
    
    public void toggleconsole()
    {
            showconsole = !showconsole;
            if(showconsole) paused = true;
    }
    
    public void consoletype(String s)
    {
            consoleinput = "" + consoleinput + s + "";
    }
    
    public void clearconsole()
    {
            consolelog = new String[100];
            for(int i=0; i<100; i++) consolelog[i] = "";
            consolelog[0] = "JIGSAW Build 7 5/5/2008 by Mike Turley, a MOTUsoft project";
    }
    
    public void consolereturn()
    {
            consolescrollup = 0;
            lastconsoleinput = consoleinput;
            consoleprintln("] " + consoleinput);
            if(consoleinput.equals("force exit"))
            {
                    System.exit(0);
            }
            else if(consoleinput.equals("help"))
            {
                    consoleprintln("> JIGSAW Console Help");
                    consoleprintln("> the left and right arrows recall the last entered command.");
                    consoleprintln("> the up and down arrows scroll through the log, a maximum of 100 lines.");
                    consoleprintln("> the ESC key closes the console.  Just press C while paused to bring it back.");
                    consoleprintln("> COMMANDS (basic commands only.  for a full list type \"clist\")");
                    consoleprintln(">   \"force exit\" forces the program to exit.  Careful!");
                    consoleprintln(">   \"clear\" clears this console clutter.");
                    consoleprintln(">   \"about\" tells you about JIGSAW.");
                    consoleprintln(">   \"help\" shows you this again.");
            }
            else if(consoleinput.equals("clist"))
            {
                    consoleprintln("> Complete JIGSAW Console Command List");
                    consoleprintln(">   about - version, history, etc. information about JIGSAW.");
                    consoleprintln(">   antialiasing - turns antialiasing on or off (smoother but slower)");
                    consoleprintln(">   clear - restores console to initial state");
                    consoleprintln(">   clist - shows command list");
                    consoleprintln(">   cls - same as clear (for you DOS users)");
                    consoleprintln(">   force exit - forces an unconditional JVM exit. [System.exit(0)]");
                    consoleprintln(">   help - short help message");
                    consoleprintln(">   wireframes - turns wireframes on or off (green lines and no fills)");
            }
            else if(consoleinput.equals("about"))
            {
                    consoleprintln("> J.I.G.S.A.W. = Java Isometric Graphics System Awesome Wow");
                    consoleprintln("> JIGSAW is Mike Turley's high school brainchild.");
                    consoleprintln("> It is a pseudo-three-dimensional graphics engine based solely on");
                    consoleprintln("> two-dimensional math, written entirely in Java and using only 2D Graphics APIs.");
                    consoleprintln("> He began work on it to prove he'd actually learned something in precalculus.");
                    consoleprintln("> Since then he has been improving it gradually in free time.");
                    consoleprintln("> He hopes it will soon be suitable as a simple game development platform.");
                    consoleprintln("> Other possible applications of JIGSAW include simulation.");
            }
            else if(consoleinput.equals("clear") || consoleinput.equals("cls"))
            {
                    clearconsole();
            }
            else if(consoleinput.equals("wireframes"))
            {
                    consoleprintln("> wireframe mode turns off all filling and changes edge colors to green.");
                    consoleprintln("> this makes models transparent and is useful for model debugging.");
                    if(WIREFRAMES) consoleprintln("> wireframes are currently ON");
                    else consoleprintln("> wireframes are currently OFF");
                    consoleprintln("> command syntax: wireframes <on/off>");
            }
            else if(consoleinput.equals("wireframes on"))
            {
                    WIREFRAMES = true;
                    consoleprintln("WIREFRAMES have been turned ON.");
            }
            else if(consoleinput.equals("wireframes off"))
            {
                    WIREFRAMES = false;
                    consoleprintln("WIREFRAMES have been turned OFF.");
            }
            else if(consoleinput.equals("antialiasing"))
            {
                    consoleprintln("> antialiasing makes edges smoother but slows the engine down drastically.");
                    if(ANTIALIASING_ON) consoleprintln("> antialiasing is currently ON");
                    else consoleprintln("> antialiasing is currently OFF");
                    consoleprintln("> command syntax: antialiasing <on/off>");
            }
            else if(consoleinput.equals("antialiasing on"))
            {
                    ANTIALIASING_ON = true;
                    dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    consoleprintln("ANTIALIASING has been turned ON.");
            }
            else if(consoleinput.equals("antialiasing off"))
            {
                    ANTIALIASING_ON = false;
                    dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
                    consoleprintln("ANTIALIASING has been turned OFF.");
            }
            else consoleprintln("> no such command.  type \"clist\" for a command list.");
            consoleinput = "";
    }
    
    public void consoleprintln(String s)
    {
            for(int i=98; i>=0; i--)
            {
                    consolelog[i+1] = consolelog[i];
            }
            consolelog[0] = s;
    }
    
    public void consolebackspace()
    {
            consoleinput = consoleinput.substring(0,consoleinput.length() - 1);
    }
    
    public void consolelast()
    {
            consoleinput = lastconsoleinput;
    }
    
    public void keyPressed(KeyEvent e)
    {
        if(paused && showconsole)
        {
                switch(e.getKeyCode())
                 {
                        case KeyEvent.VK_A : consoletype("a"); break;
                        case KeyEvent.VK_B : consoletype("b"); break;
                        case KeyEvent.VK_C : consoletype("c"); break;
                        case KeyEvent.VK_D : consoletype("d"); break;
                        case KeyEvent.VK_E : consoletype("e"); break;
                        case KeyEvent.VK_F : consoletype("f"); break;
                        case KeyEvent.VK_G : consoletype("g"); break;
                        case KeyEvent.VK_H : consoletype("h"); break;
                        case KeyEvent.VK_I : consoletype("i"); break;
                        case KeyEvent.VK_J : consoletype("j"); break;
                        case KeyEvent.VK_K : consoletype("k"); break;
                        case KeyEvent.VK_L : consoletype("l"); break;
                        case KeyEvent.VK_M : consoletype("m"); break;
                        case KeyEvent.VK_N : consoletype("n"); break;
                        case KeyEvent.VK_O : consoletype("o"); break;
                        case KeyEvent.VK_P : consoletype("p"); break;
                        case KeyEvent.VK_Q : consoletype("q"); break;
                        case KeyEvent.VK_R : consoletype("r"); break;
                        case KeyEvent.VK_S : consoletype("s"); break;
                        case KeyEvent.VK_T : consoletype("t"); break;
                        case KeyEvent.VK_U : consoletype("u"); break;
                        case KeyEvent.VK_V : consoletype("v"); break;
                        case KeyEvent.VK_W : consoletype("w"); break;
                        case KeyEvent.VK_X : consoletype("x"); break;
                        case KeyEvent.VK_Y : consoletype("y"); break;
                        case KeyEvent.VK_Z : consoletype("z"); break;
                        case KeyEvent.VK_0 : consoletype("0"); break;
                        case KeyEvent.VK_1 : consoletype("1"); break;
                        case KeyEvent.VK_2 : consoletype("2"); break;
                        case KeyEvent.VK_3 : consoletype("3"); break;
                        case KeyEvent.VK_4 : consoletype("4"); break;
                        case KeyEvent.VK_5 : consoletype("5"); break;
                        case KeyEvent.VK_6 : consoletype("6"); break;
                        case KeyEvent.VK_7 : consoletype("7"); break;
                        case KeyEvent.VK_8 : consoletype("8"); break;
                        case KeyEvent.VK_9 : consoletype("9"); break;
                        case KeyEvent.VK_SPACE : consoletype(" "); break;
                        case KeyEvent.VK_ENTER : consolereturn(); break;
                        case KeyEvent.VK_BACK_SPACE : consolebackspace(); break;
                        case KeyEvent.VK_LEFT : consolelast(); break;
                        case KeyEvent.VK_RIGHT : consolelast(); break;
                        case KeyEvent.VK_UP : consolescrollup++; break;
                        case KeyEvent.VK_DOWN : consolescrollup--; break;
                        case KeyEvent.VK_ESCAPE : toggleconsole(); break;
                 }
        }
        if(!paused)
        {
                switch(e.getKeyCode())
                {
                        case KeyEvent.VK_Q : System.exit(0); break;
                        case KeyEvent.VK_LEFT : rotatingleft = true; break;
                        case KeyEvent.VK_RIGHT : rotatingright = true; break;
                        case KeyEvent.VK_UP : rotatingup = true; break;
                        case KeyEvent.VK_DOWN : rotatingdown = true; break;
                        case KeyEvent.VK_W : movingW = true; break;
                        case KeyEvent.VK_S : movingS = true; break;
                        case KeyEvent.VK_A : movingA = true; break;
                        case KeyEvent.VK_D : movingD = true; break;
                        case KeyEvent.VK_R : zoomingin = true; break;
                        case KeyEvent.VK_F : zoomingout = true; break;
                        case KeyEvent.VK_H : slide(7.0,7.0,0.0,0.5,800.0); break;
                        case KeyEvent.VK_J : slide(7.0,7.0,theta+3*(Math.PI*2),0.5,800.0); break;
                        case KeyEvent.VK_P : togglepause(); break;
                        case KeyEvent.VK_C : toggleconsole(); break;
                }
        } else if(!showconsole)
        {
                 switch(e.getKeyCode())
                 {
                        case KeyEvent.VK_C : toggleconsole(); break;
                        case KeyEvent.VK_P : togglepause(); break;
                 }
        }
        // these 4 lines should be in drawToImage, but need to be immediately done after keypress
        if(ctx<0) ctx = 0;
        if(ctx>tileswide-1) ctx = tileswide-1;
        if(cty<0) cty = 0;
        if(cty>tileslong-1) cty = tileslong-1;
    }
    public void keyReleased(KeyEvent e)
    {
        switch(e.getKeyCode())
        {
            case KeyEvent.VK_LEFT : rotatingleft = false; break;
            case KeyEvent.VK_RIGHT : rotatingright = false; break;
            case KeyEvent.VK_UP : rotatingup = false; break;
            case KeyEvent.VK_DOWN : rotatingdown = false; break;
            case KeyEvent.VK_R : zoomingin = false; break;
            case KeyEvent.VK_F : zoomingout = false; break;
            case KeyEvent.VK_W : movingW = false; break;
            case KeyEvent.VK_S : movingS = false; break;
            case KeyEvent.VK_A : movingA = false; break;
            case KeyEvent.VK_D : movingD = false; break;
        }
    }
    
    public void keyTyped(KeyEvent e)
    {
        
    }
    
    public void mouseMoved(MouseEvent e)
    {
        mxc = e.getX();
        myc = e.getY();
    }
    
    public void mouseClicked(MouseEvent e)
    {
        mxc = e.getX();
        myc = e.getY();
    }
    public void mouseDragged(MouseEvent e)
    {
        mxc = e.getX();
        myc = e.getY();
    }
    
    public void mouseEntered(MouseEvent e)
    {
        mxc = e.getX();
        myc = e.getY();
    }
    
    public void mouseExited(MouseEvent e)
    {
        mxc = e.getX();
        myc = e.getY();
    }
    
    public void mousePressed(MouseEvent e)
    {
        mxc = e.getX();
        myc = e.getY();
    }
    
    public void mouseReleased(MouseEvent e)
    {
        mxc = e.getX();
        myc = e.getY();
    }
    
    public void moveCenterW()
    {
        if(theta>=0 && theta<((Math.PI)/2)) goToTile(ctx,cty+1);
        if(theta>=((Math.PI)/2) && theta<(Math.PI)) goToTile(ctx+1,cty);
        if(theta>=(Math.PI) && theta<((Math.PI)*1.5)) goToTile(ctx,cty-1);
        if(theta>=((Math.PI)*1.5) && theta<((Math.PI)*2)) goToTile(ctx-1,cty);
    }
    public void moveCenterS()
    {
        if(theta>=0 && theta<((Math.PI)/2)) goToTile(ctx,cty-1);
        if(theta>=((Math.PI)/2) && theta<(Math.PI)) goToTile(ctx-1,cty);
        if(theta>=(Math.PI) && theta<((Math.PI)*1.5)) goToTile(ctx,cty+1);
        if(theta>=((Math.PI)*1.5) && theta<((Math.PI)*2)) goToTile(ctx+1,cty);
    }
    public void moveCenterA()
    {
        if(theta>=0 && theta<((Math.PI)/2)) goToTile(ctx-1,cty);
        if(theta>=((Math.PI)/2) && theta<(Math.PI)) goToTile(ctx,cty+1);
        if(theta>=(Math.PI) && theta<((Math.PI)*1.5)) goToTile(ctx+1,cty);
        if(theta>=((Math.PI)*1.5) && theta<((Math.PI)*2)) goToTile(ctx,cty-1);
    }
    public void moveCenterD()
    {
        if(theta>=0 && theta<((Math.PI)/2)) goToTile(ctx+1,cty);
        if(theta>=((Math.PI)/2) && theta<(Math.PI)) goToTile(ctx,cty-1);
        if(theta>=(Math.PI)&& theta<((Math.PI)*1.5)) goToTile(ctx-1,cty);
        if(theta>=((Math.PI)*1.5) && theta<((Math.PI)*2)) goToTile(ctx,cty+1);
    }
    
    public void act()
    {
        vtheta = (1-yscale)*(Math.PI/2);
        hfactor = Math.sin(vtheta);
        
        // temporary processes!
        bht = board[10][8].platforms[2].getBaseHeight();
        bht += redcubeVel;
        if(bht>1.5) redcubeAcc = 0-Math.abs(redcubeAcc);
        if(bht<1.5) redcubeAcc = Math.abs(redcubeAcc);
        redcubeVel += redcubeAcc;
        board[10][8].platforms[2].setBaseHeight(bht);
        
        board[10][8].platforms[2].rotDT += 0.1;
        while(board[10][8].platforms[2].rotDT >= 2*Math.PI) board[10][8].platforms[2].rotDT -= 2*Math.PI;
        // end temp processes
        
        // slide to target
        if(cx < targetcx) cx += (targetcx - cx)/10;
        if(cy < targetcy) cy += (targetcy - cy)/10;
        if(cx > targetcx) cx -= (cx - targetcx)/10;
        if(cy > targetcy) cy -= (cy - targetcy)/10;
        if(sliding)
        {
            if(theta < targettheta) theta += (targettheta - theta)/10;
            if(theta > targettheta) theta -= (theta - targettheta)/10;
            if(yscale < targetyscale) yscale += (targetyscale - yscale)/10;
            if(yscale > targetyscale) yscale -= (yscale - targetyscale)/10;
            if(tilesize < targetzoom) tilesize += (targetzoom - tilesize)/10;
            if(tilesize > targetzoom) tilesize -= (tilesize - targetzoom)/10;
        }
        if(sliding && theta > targettheta-0.05 && theta < targettheta+0.05 &&
           yscale > targetyscale-0.05 && yscale < targetyscale+0.05 &&
           tilesize > targetzoom-20.0 && tilesize < targetzoom+20.0)
        {
            sliding = false;
            theta = targettheta;
            yscale = targetyscale;
            tilesize = targetzoom;
        }
        
        if(!movingW && !movingA && !movingS && !movingD) moveinc = 0.0;
        if(movingW || movingA || movingS || movingD) moveinc += movespeed;
        if(moveinc >= 1.0 && !homing)
        {
            moveinc = 0.0;
            if(movingW) moveCenterW();
            if(movingA) moveCenterA();
            if(movingS) moveCenterS();
            if(movingD) moveCenterD();
        }
        if(rotatingright)
        {
            if(!homing) stopsliding();
            theta -= rotatespeed;
        }
        if(rotatingleft)
        {
            if(!homing) stopsliding();
            theta += rotatespeed;
        }
        if(rotatingup)
        {
            if(!homing) stopsliding();
            yscale += rotatespeed;
            if(yscale > 1.0) yscale = 1.0;
        }
        if(rotatingdown)
        {
            if(!homing) stopsliding();
            yscale -= rotatespeed;
            if(yscale < 0.1) yscale = 0.1;
        }
        if(!sliding)
        {
            while(theta > (2*Math.PI)) theta -= (2*Math.PI);
            while(theta < 0) theta += (2*Math.PI);
        }
        if(zoomingin)
        {
            tilesize += zoomspeed;
        }
        if(zoomingout)
        {
            tilesize -= zoomspeed;
            int i=0;
            if(tilesize <= 0)
            {
                while(true)
                {
                    tilesize += zoomspeed*(10^i);
                    i++;
                    tilesize -= zoomspeed*(10^i);
                    if(tilesize>0) break;
                }
            }
        }
        
    }
    
    public void drawPaused()
    {
            AlphaComposite faded75 = AlphaComposite.getInstance(
                 AlphaComposite.SRC_OVER,0.75f);
            AlphaComposite faded25 = AlphaComposite.getInstance(
                 AlphaComposite.SRC_OVER,0.25f);
            dbg.setComposite(faded75);
            dbg.setColor(Color.black);
            dbg.fillRect(0,0,getSize().width,getSize().height);
            AlphaComposite fullcomposite = AlphaComposite.getInstance(
                 AlphaComposite.SRC_OVER,1.0f);
            dbg.setComposite(fullcomposite);
            dbg.setColor(Color.black);
            dbg.fillRoundRect((int)(getSize().width/2)-50,(int)(getSize().height/2)-15,100,30,5,5);
            dbg.setColor(Color.red);
            dbg.drawRoundRect((int)(getSize().width/2)-50,(int)(getSize().height/2)-15,100,30,5,5);
            dbg.drawString("PAUSED",(int)(getSize().width/2)-23,(int)(getSize().height/2)+4);
            if(showconsole)
            {
                    dbg.setComposite(faded25);
                    dbg.setColor(Color.black);
                    dbg.fillRoundRect(10,10,650,350,5,5);
                    dbg.setComposite(fullcomposite);
                    dbg.setColor(Color.red);
                    dbg.drawLine(10,30,660,30);
                    dbg.drawString("JIGSAW Console - type \"help\" for help, ESC to close",15,25);
                    dbg.drawRoundRect(10,10,650,350,5,5);
                    int line = 0;
                    if(consolescrollup > 95) consolescrollup = 95;
                    if(consolescrollup < 0) consolescrollup = 0;
                    for(int i=consolescrollup; i<consolescrollup+24; i++)
                    {
                            if(i<100) dbg.drawString(consolelog[i],15,340-(13*line));
                            line++;
                    }
                    dbg.drawString("] " + consoleinput + "<",20,355);
                    if(consolescrollup > 0)
                    {
                            dbg.fillPolygon(new int[]{650,645,655},new int[]{350,345,345},3);
                            if(consolescrollup<10) dbg.drawString(""+consolescrollup,645,340);
                            else dbg.drawString(""+consolescrollup,640,340);
                    }
            }
    }
    
    public void drawToImage()
    {
        dbg.setColor(Color.black);
        dbg.fillRect(0,0,getSize().width, getSize().height);
        
        radius = Math.sqrt(2*((int)tilesize^2));
        
        dx = (Math.cos(theta)*radius);
        dy = (Math.sin(theta)*radius);
        if(cy==-1) cy = getSize().height / 2;
        if(cx==-1) cx = getSize().width / 2;
        if(targetcy==-1) targetcy = cy;
        if(targetcx==-1) targetcx = cx;
        int[] xvals, yvals;
        double[] spc = spacing();
        spacex = spc[0];
        spacey = spc[1];
        
        tileDrawOrder();
        
        // draw out the tiles!!!
        
        drawCells();
        
        if(WIREFRAMES)
        {
            for(int t=0; t<tileswide*tileslong; t++)
            {
                int tx = draworder[t][0];
                int ty = draworder[t][1];
                double[] pxl = getTilePosition(tx,ty);
                double icx = pxl[0]; double icy = pxl[1];
                xvals = new int[]{ (int)(icx+dx) , (int)(icx+dy) ,
                    (int)(icx-dx) , (int)(icx-dy) };
                yvals = new int[]{ (int)(icy-(dy*yscale)) , (int)(icy+(dx*yscale)) ,
                    (int)(icy+(dy*yscale)) , (int)(icy-(dx*yscale)) };
                if(ty==cty && tx==ctx)
                {
                    // shade the center tile yellow
                    dbg.setColor(Color.yellow);
                    dbg.fillPolygon(xvals,yvals,4);
                }
                // outline tile green
                dbg.setColor(Color.green);
                if(ty==0 && tx==0) dbg.setColor(Color.red);
                dbg.drawPolygon(xvals,yvals,4);
            }
        }
        
        // done drawing out the tiles!!!
        
        
        if(DEV_MODE)
        {
            double icx, icy;
            icx = cx + (spacex/2);
            icy = cy + ((spacey/2))*yscale;
            dbg.setColor(Color.red);
            dbg.drawLine((int)cx,(int)cy,(int)icx,(int)icy);
            icx = cx + 0 - (spacey/2);
            icy = cy + (0 + (spacex/2))*yscale;
            dbg.setColor(Color.blue);
            dbg.drawLine((int)cx,(int)cy,(int)icx,(int)icy);
            dbg.setFont(new Font("Courier",Font.BOLD,14));
            dbg.setColor(Color.red);
            dbg.drawString("DEVELOPER'S MODE :: Paused = " + paused,10,20);
            dbg.drawString("Rotational Center (yellow): X=" + ctx + ", Y=" + cty, 10, 40);
            dbg.drawString("Angle of Rotation: " + ((int)((theta/(Math.PI))*1000)/1000.0) + "pi = " + ((int)(theta*1000)/1000.0) + " radians ",10,60);
            dbg.drawString("Vertical (Y) Scale: " + ((int)(yscale*1000)/1000.0) ,10,80);
            dbg.drawString("Orientation Code: " + getDrawOrientation() + ", Tile Size: " + tilesize, 10,100);
            dbg.drawString("SLIDING? " + sliding,10,140);
            dbg.drawString("Arrows: Rotate   W,A,S,D: Move",getSize().width-300,20);
            dbg.drawString("R,F: Zoom   Q: Quit",getSize().width-300,40);
            dbg.drawString("JIGSAW (c) 2008 Michael J. Turley",10,getSize().height-20);
            dbg.drawString("Build 7 (05/05/2008)",getSize().width-175,getSize().height-20);
            if(redcubeVel < 0) dbg.drawString("Red Cube: " + ((int)(redcubeVel*1000)/1000.0) + "vel , " + ((int)(redcubeAcc*1000)/1000.0) + "acc , " + ((int)(bht*1000)/1000.0) + "bht, " + ((int)(board[10][8].platforms[2].rotDT*1000)/1000.0) + "rotDT",10,120);
            if(redcubeVel >= 0) dbg.drawString("Red Cube:  " + ((int)(redcubeVel*1000)/1000.0) + "vel , " + ((int)(redcubeAcc*1000)/1000.0) + "acc , " + ((int)(bht*1000)/1000.0) + "bht, " + ((int)(board[10][8].platforms[2].rotDT*1000)/1000.0) + "rotDT",10,120);
            // draworder labels:
            if(LABEL_DRAWORDER)
                for(int i=0; i<draworder.length; i++)
                {
                    double[] pos = getTilePosition(draworder[i][0],draworder[i][1]);
                    dbg.drawString(""+i+"",(int)(pos[0]-dx),(int)(pos[1]-(yscale*dy)));
                }
            // draworder labels ^
        }        
    }
    
    public void togglepause()
    {
            paused = !paused;
    }
    
    public int[][] corners(int tx, int ty)
    {
        return corners(tx,ty,0.0,0.0,0.0);
    }
    
    public int[][] corners(int tx, int ty, double posDX, double posDY)
    {
        return corners(tx,ty,posDX,posDY,0.0);
    }
    
    public int[][] corners(int tx, int ty, double posDX, double posDY, double rotDT)
    {
        double[] pxl = getTilePosition(tx,ty,posDX,posDY);
        if(rotDT != 0.0)
        {
            dxT = (Math.cos(theta+rotDT)*radius);
            dyT = (Math.sin(theta+rotDT)*radius);
        } else {
            dxT = dx;
            dyT = dy;
        }
        double icx = pxl[0]; double icy = pxl[1];
        double[] corner0 = new double[]{ (icx+dxT) , (icy-(dyT*yscale)) };
        double[] corner1 = new double[]{ (icx+dyT) , (icy+(dxT*yscale)) };
        double[] corner2 = new double[]{ (icx-dxT) , (icy+(dyT*yscale)) };
        double[] corner3 = new double[]{ (icx-dyT) , (icy-(dxT*yscale)) };
        double[][] tempcorners = new double[4][2];
        
        tempcorners[3] = corner0;
        double minx = corner0[0];
        if(corner1[0] < minx) { minx = corner1[0]; tempcorners[3] = corner1; }
        if(corner2[0] < minx) { minx = corner2[0]; tempcorners[3] = corner2; }
        if(corner3[0] < minx) { minx = corner3[0]; tempcorners[3] = corner3; }
        tempcorners[1] = corner0;
        double maxx = corner0[0];
        if(corner1[0] > maxx) { maxx = corner1[0]; tempcorners[1] = corner1; }
        if(corner2[0] > maxx) { maxx = corner2[0]; tempcorners[1] = corner2; }
        if(corner3[0] > maxx) { maxx = corner3[0]; tempcorners[1] = corner3; }
        tempcorners[0] = corner0;
        double maxy = corner0[1];
        if(corner1[1] > maxy) { maxy = corner1[1]; tempcorners[0] = corner1; }
        if(corner2[1] > maxy) { maxy = corner2[1]; tempcorners[0] = corner2; }
        if(corner3[1] > maxy) { maxy = corner3[1]; tempcorners[0] = corner3; }
        tempcorners[2] = corner0;
        double miny = corner0[1];
        if(corner1[1] < miny) { miny = corner1[1]; tempcorners[2] = corner1; }
        if(corner2[1] < miny) { miny = corner2[1]; tempcorners[2] = corner2; }
        if(corner3[1] < miny) { miny = corner3[1]; tempcorners[2] = corner3; }
        
        int[][] corners = new int[][] { {(int)(tempcorners[0][0]),(int)(tempcorners[0][1])},
                                {(int)(tempcorners[1][0]),(int)(tempcorners[1][1])},
                                {(int)(tempcorners[2][0]),(int)(tempcorners[2][1])},
                                {(int)(tempcorners[3][0]),(int)(tempcorners[3][1])} };
                                
        return corners;
    }
    
    public void tileDrawOrder()
    { // sort by lowest Y pixel pos?
        double[][] presortPxl = new double[tileswide*tileslong][2];
        int[][] presortTile = new int[tileswide*tileslong][2];
        if(getDrawOrientation() != draworientation)
        {
            int i=0;
            draworientation = getDrawOrientation();
            // step 1: gather all pixel locations
            for(int h=0; h<tileswide; h++)
                for(int v=0; v<tileslong; v++)
                {
                    presortPxl[i] = getTilePosition(h,v);
                    presortTile[i] = new int[]{h,v};
                    i++;
                }
            // step 2: sort with highest presortPxl[?][2] first
            boolean sorted = false;
            while(!sorted)
            {
                // sort the tiles
                for(int j=0; j<(presortPxl.length-1); j++)
                {
                    if(presortPxl[j][1] < presortPxl[j+1][1])
                    {
                        double[] tempPxl = presortPxl[j];
                        int[] tempTile = presortTile[j];
                        presortPxl[j] = presortPxl[j+1];
                        presortPxl[j+1] = tempPxl;
                        presortTile[j] = presortTile[j+1];
                        presortTile[j+1] = tempTile;
                        //j=0;
                    }
                }
                // done sorting, check the sort
                sorted = true;
                for(int c=1; c<(tileswide*tileslong); c++)
                {
                    if(presortPxl[c][1] > presortPxl[c-1][1]) sorted = false;
                }
                // done checking the sort
                i=0;
                draworder = new int[presortTile.length][2];
                for(int r=(presortTile.length-1); r>=0; r--)
                {
                    draworder[i] = presortTile[r];
                    i++;
                }
            }
        }
    }

    public int getDrawOrientation()
    {
        return (int)(theta/((Math.PI)/16)) + 1;
    }
    
    public double[] getTilePosition(int h, int v)
    {
        return getTilePosition(h,v,0.0,0.0);
    }
    
    public double[] getTilePosition(int h, int v, double posDX, double posDY)
    {
        double icx, icy;
        double dv, dh;
        dv = v-cty + posDY;
        dh = h-ctx + posDX;
        icx = cx + spacex*dv - spacey*dh;
        icy = cy + (spacey*dv + spacex*dh)*yscale;
        return new double[]{icx,icy};
    }
    
    public double[] get2dCoords(double x, double y, double z)
    {
        double icx, icy;
        double dv, dh;
        dv = y-cty;
        dh = x-ctx;
        icx = cx + spacex*dv - spacey*dh;
        icy = cy + (spacey*dv + spacex*dh)*yscale;
        double hmult = (4*Math.PI/9);
        int bh = (int)((z)*(hfactor)*(radius*hmult));
        icy -= bh;
        return new double[]{icx,icy};
    }
    
    public double[] get3dCoords(double x, double y)
    {
        return get3dCoords(x,y,0.0);
    }
    
    public double[] get3dCoords(double x, double y, double targetheight)
    {
        double hmult = (4*Math.PI/9);
        int bh = (int)((targetheight)*(hfactor)*(radius*hmult));
        double icx, icy;
        
        icx = x; icy = y;
        icx += bh;
        icy -= cy;
        icy /= yscale;
        icx -= cx;
        icy += (spacey*cty);
        icy += (spacex*ctx);
        icx -= (spacey*ctx);
        icx += (spacex*cty);
        double fx = ((((icy/spacey)-(icx/spacex))*(spacex*spacey))/(spacex*spacex+spacey*spacey));
        double fy = ((icy-(spacex*fx))/spacey);
        return new double[]{fx,fy};
    }
    
    public double[] spacing(double dxT, double dyT)
    {
        double p2x = cx+dxT;
        double p2y = cy-(dyT);
        double p1x = cx+dyT;
        double p1y = cy+(dxT);
        return new double[]{ p2x-p1x , p2y-p1y };
    }
    
    public double[] spacing()
    {
        double p2x = cx+dx;
        double p2y = cy-(dy);
        double p1x = cx+dy;
        double p1y = cy+(dx);
        return new double[]{ p2x-p1x , p2y-p1y };
    }
    
    public void drawToScreen()
    {
        Graphics g;
        
        try
        {
            g = this.getGraphics();
            
            if(g != null && img != null)
            {
                g.drawImage(img, 0, 0, null);   
            }
            g.dispose();
        }
        catch(Exception e)
        {
            System.out.println("ERR: @ drawToScreen():");
            System.out.println(e);
        }
    }
}