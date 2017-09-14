package Ads;

import java.util.Random;

/**
 * Created by Sagar on 9/14/2017.
 */
//this class randomly selects which asd to show
public class SelectWhichAdTOShow {
    public int select_which_ad(int limit){
        int max = 1;
        int min = limit;
        Random rand=new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive

        return rand.nextInt((max - min) + 1) + min;
    }
}
