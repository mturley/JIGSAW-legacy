/**
 * @(#)JIGSandbox.java
 *
 * Java Isometric Graphics System Awesome Wow
 * J    I         G        S      A       W
 *
 * @author Michael J. Turley
 * @version 7.00 2008/5/5
 */
 
import javax.swing.*;
import java.awt.*;
import java.util.*;
 
public class JIGSandbox extends JFrame
{
	Container C;
	JIGEngine it;
	
	public JIGSandbox()
	{
        it = new JIGEngine();
		C = getContentPane();
    	C.setLayout( new BorderLayout() );   
        C.add(it);
    	setUndecorated(true); 
    	setIgnoreRepaint(true);
    	pack();
    	setResizable(false);
    	setVisible(true);
    	it.main.start();
	}
	//JIGPlatform p = new JIGPlatform();
	//	p.setTextures("tF.gif","tL.gif","tB.gif","tR.gif","tT.gif");
	// TESTING STUB
	public static void main(String[] args)
	{
		new JIGSandbox();
	}
}