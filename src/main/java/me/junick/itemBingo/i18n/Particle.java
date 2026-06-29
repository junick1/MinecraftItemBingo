package me.junick.itemBingo.i18n;

/**
 * Korean particle (조사) selection. The correct particle after a noun depends on
 * whether the noun's final syllable carries a 받침 (final consonant): a Hangul
 * syllable block {@code S} (U+AC00..U+D7A3) has a 받침 iff {@code (S - 0xAC00) % 28 != 0}.
 *
 * <p>Used by {@link Messages} to resolve the {@code {noun:withBatchim/withoutBatchim}}
 * placeholder filter, e.g. {@code {item:을/를}}. Nouns that don't end in a Hangul
 * syllable (Latin letters, digits, symbols) default to the consonant form, which
 * is the safer choice for the bracketed forms we use.</p>
 */
public final class Particle {
    private Particle() {}

    /** ㄹ jongseong index within a Hangul syllable block (used for the (으)로 exception). */
    private static final int JONG_RIEUL = 8;

    /**
     * Returns {@code withBatchim} when {@code noun} ends in a 받침, else
     * {@code withoutBatchim}. Implements the (으)로 exception: a ㄹ-final noun takes
     * the vowel form {@code 로} rather than {@code 으로}.
     */
    public static String pick(String noun, String withBatchim, String withoutBatchim) {
        Boolean batchim = hasBatchim(noun);
        if (batchim == null) {
            return withBatchim; // non-Hangul ending: default to consonant form
        }
        if (batchim) {
            // (으)로: ㄹ behaves like a vowel here.
            if ("로".equals(withoutBatchim) && isRieulFinal(noun)) {
                return withoutBatchim;
            }
            return withBatchim;
        }
        return withoutBatchim;
    }

    public static String eunNeun(String noun) { return pick(noun, "은", "는"); }
    public static String iGa(String noun)     { return pick(noun, "이", "가"); }
    public static String eulReul(String noun) { return pick(noun, "을", "를"); }
    public static String gwaWa(String noun)   { return pick(noun, "과", "와"); }
    public static String euRo(String noun)    { return pick(noun, "으로", "로"); }

    /**
     * {@code null} if the last character isn't a Hangul syllable; otherwise whether
     * that syllable has a 받침.
     */
    private static Boolean hasBatchim(String noun) {
        if (noun == null || noun.isEmpty()) return null;
        char ch = noun.charAt(noun.length() - 1);
        if (ch < 0xAC00 || ch > 0xD7A3) return null;
        return (ch - 0xAC00) % 28 != 0;
    }

    private static boolean isRieulFinal(String noun) {
        char ch = noun.charAt(noun.length() - 1);
        if (ch < 0xAC00 || ch > 0xD7A3) return false;
        return (ch - 0xAC00) % 28 == JONG_RIEUL;
    }
}
