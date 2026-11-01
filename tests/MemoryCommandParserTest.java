import com.jarvis.kisisel.MemoryCommandParser;

public final class MemoryCommandParserTest {
    static int passed=0;
    static void expect(String original,String expected){
        String actual=MemoryCommandParser.extractSaveNote(original);
        if(expected==null ? actual!=null : !expected.equals(actual)) {
            throw new AssertionError("Input: " + original + " | expected="+expected+" actual="+actual);
        }
        passed++;
    }
    public static void main(String[] args) {
        expect("Pandora, şu bilgiyi hafızana kaydet: PANDORA 6 testimiz başarılı.","PANDORA 6 testimiz başarılı.");
        expect("Pandora, şu bilgiyi hafızana kaydet: PANDORA 6 testimiz başarılı.\nBekl", "PANDORA 6 testimiz başarılı. Bekl");
        expect("HEY PANDORA: HAFIZANA KAYDET yeni bir bilgi", "yeni bir bilgi");
        expect("Hey Pandora hafızana kaydet: test notu", "test notu");
        expect("hafızaya kaydet: deneme", "deneme");
        expect("Pandora, bunu hatırla: yeni not", "yeni not");
        expect("Pandora, şu bilgiyi hafızana kaydet", "");
        expect("Pandora, beni arar mısın?", null);
        expect("Mısır tarihini araştır", null);
        expect("HAFIZANA NOT ET: buluşma", "buluşma");
        expect("Pandora, şu bilgiyi hafızana kaydet: ilk satır\nikinci satır", "ilk satır ikinci satır");
        System.out.println("BAŞARILI: "+passed+" hafıza komutu testi");
    }
}
