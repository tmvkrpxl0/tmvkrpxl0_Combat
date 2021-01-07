package tmvkrpxl0.src;

import org.apache.commons.lang.ArrayUtils;

public class Test {
    public static void main(String[] args){
        final String[] targetCommands = new String[]{"lift", "goback", "tnt", "homing"};
        final String[] nonTargetCommands = new String[]{"target", "arrow", "fishingmode", "jump"};
        final String[] allCommands = (String[]) ArrayUtils.addAll(targetCommands, nonTargetCommands);
        System.out.println(allCommands);
    }
}
