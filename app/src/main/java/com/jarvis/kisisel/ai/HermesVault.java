package com.jarvis.kisisel.ai;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
/** Android Keystore encrypted token; nothing embedded in APK or build files. */
public final class HermesVault {
  private static final String KEY="pandora_hermes_local_v1";
  private static final String STORE="pandora_hermes_vault";
  private HermesVault(){}
  private static SecretKey secret() throws Exception {
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    java.security.Key existing=ks.getKey(KEY,null);
    if(existing instanceof SecretKey)return (SecretKey)existing;
    KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    gen.init(new KeyGenParameterSpec.Builder(KEY,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setRandomizedEncryptionRequired(true).build());
    return gen.generateKey();
  }
  public static boolean put(Context context,String raw) {
    try {
      Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,secret());
      byte[] encoded=c.doFinal(raw.getBytes(StandardCharsets.UTF_8));
      context.getSharedPreferences(STORE,0).edit().putString("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP))
        .putString("value",Base64.encodeToString(encoded,Base64.NO_WRAP)).apply();return true;
    }catch(Exception ex){return false;}
  }
  public static boolean exists(Context c){return c.getSharedPreferences(STORE,0).contains("value");}
  public static String get(Context context) {
    try {
      SharedPreferences p=context.getSharedPreferences(STORE,0);
      String value=p.getString("value",null), iv=p.getString("iv",null);
      if(value==null || iv==null)return "";
      Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
      return new String(c.doFinal(Base64.decode(value,Base64.NO_WRAP)),StandardCharsets.UTF_8);
    } catch(Exception ex){return "";}
  }
  public static void clear(Context c){c.getSharedPreferences(STORE,0).edit().clear().apply();}
}
