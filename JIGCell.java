import java.util.*;

public class JIGCell extends JIGObject
{
    boolean walkable;
    boolean blocksview;
    double efheight;
    JIGPlatform[] platforms;
    JIGObject[] contents;
    
    public JIGCell()
    {
        walkable = false;
        blocksview = true;
        efheight = 0.0;
        contents = new JIGObject[0];
        platforms = new JIGPlatform[1];
        platforms[0] = (new JIGPlatform());
    }
    
    public void addPlatform(JIGPlatform p)
    {
        p.shown = true;
        JIGPlatform[] tps = platforms;
        platforms = new JIGPlatform[tps.length+1];
        for(int i=0; i<tps.length; i++)
            platforms[i] = tps[i];
        platforms[tps.length] = p;
    }
    
    public void addEmptyPlatform()
    {
        JIGPlatform p = new JIGPlatform();
        p.shown = false;
        JIGPlatform[] tps = platforms;
        platforms = new JIGPlatform[tps.length+1];
        for(int i=0; i<tps.length; i++)
            platforms[i] = tps[i];
        platforms[tps.length] = p;
    }
}