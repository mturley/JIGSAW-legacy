import java.awt.*;
import java.io.*;
import javax.imageio.*;

public class JIGPlatform extends JIGObject
{
    boolean textured;
    boolean shown;
    Image[] textures;
    Color fillcolor;
    Color outlinecolor;
    
    public JIGPlatform()
    {
        textures = new Image[5];
        height = 0.0;
        baseheight = 0.0;
        posDX = 0.0;
        posDY = 0.0;
        rotDT = 0.0;
        textured = false;
        shown = true;
        fillcolor = Color.gray;
        outlinecolor = Color.gray;
    }
    
    public JIGPlatform(double baseI,double heightI,Color fillcolorI,Color outlinecolorI)
    {
        JIGPlatform p = new JIGPlatform();
        baseheight = baseI;
        height = heightI;
        fillcolor = fillcolorI;
        outlinecolor = outlinecolorI;
        textured = false;
    }   
    
    public JIGPlatform(double baseI,double heightI,Color fillcolorI,Color outlinecolorI,double posDXi, double posDYi, double rotDTi)
    {
        JIGPlatform p = new JIGPlatform();
        baseheight = baseI;
        height = heightI;
        fillcolor = fillcolorI;
        outlinecolor = outlinecolorI;
        textured = false;
        posDX = posDXi;
        posDY = posDYi;
        rotDT = rotDTi;
    }
    
    public void setTextures(String tF, String tL, String tB, String tR, String tT)
    {
        textures = new Image[5];
        try {
            textures[0] = ImageIO.read(new File(tF));
            textures[1] = ImageIO.read(new File(tL));
            textures[2] = ImageIO.read(new File(tB));
            textures[3] = ImageIO.read(new File(tR));
            textures[4] = ImageIO.read(new File(tT));
        } catch(java.io.IOException e) {
            System.out.println("ERR @ JIGPlatform.setTextures() :: " + e);
        }
    }
    
    public double getBaseHeight()
    {
        return baseheight;
    }
    
    public void setBaseHeight(double bhI)
    {
        baseheight = bhI;
    }
}