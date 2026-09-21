package kr.co.donationserver.util;
import net.minecraft.util.text.*;
import java.text.NumberFormat;
import java.util.Locale;
public final class Texts {
    private Texts(){}
    public static TextComponentString text(String s){return new TextComponentString(s.replace('&','§'));}
    public static String money(long n){return NumberFormat.getNumberInstance(Locale.KOREA).format(n)+"원";}
}
