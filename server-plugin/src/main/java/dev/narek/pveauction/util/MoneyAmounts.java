package dev.narek.pveauction.util;

public final class MoneyAmounts {

    private MoneyAmounts() {}

    public static long parseRaw(String raw) throws NumberFormatException {
        return Long.parseLong(raw.replace(" ", "").replace("_", ""));
    }

    /**
     * @return текст ошибки или null, если сумма допустима
     */
    public static String validate(long amount, long max) {
        if (amount < 1) {
            return "Сумма должна быть больше нуля.";
        }
        if (amount > max) {
            return "Максимальная сумма — " + GuiItems.formatPrice(max) + " $";
        }
        return null;
    }

    public static ParseResult parse(String raw, long max) {
        final long amount;
        try {
            amount = parseRaw(raw);
        } catch (NumberFormatException e) {
            return ParseResult.error("Максимальная сумма — " + GuiItems.formatPrice(max) + " $");
        }
        String error = validate(amount, max);
        if (error != null) {
            return ParseResult.error(error);
        }
        return ParseResult.ok(amount);
    }

    public record ParseResult(boolean ok, long amount, String error) {
        public static ParseResult ok(long amount) {
            return new ParseResult(true, amount, null);
        }

        public static ParseResult error(String error) {
            return new ParseResult(false, 0, error);
        }
    }
}
