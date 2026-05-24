package dev.narek.pveauction.gui.shop;

/**
 * Только для меню сдачи предметов.
 * ≤9 — один ряд по центру.
 * Чётное 10+ — два ряда поровну.
 * Нечётное — один предмет сверху по центру, остальное два ряда 50/50.
 */
public final class ShopGuiGridLayout {

    public static final int WIDTH = 9;
    public static final int CONTENT_ROWS = 4;
    public static final int CONTENT_ROW_START = 1;

    private ShopGuiGridLayout() {}

    public static int[] slotsForCount(int count) {
        return slotsForCount(count, 0, WIDTH, CONTENT_ROW_START, CONTENT_ROWS);
    }

    public static int[] slotsForCount(
            int count,
            int areaStartCol,
            int areaWidth,
            int contentRowStart,
            int contentRowsAvailable
    ) {
        if (count <= 0) {
            return new int[0];
        }
        if (count <= 9) {
            return oneRow(count, areaStartCol, areaWidth, contentRowStart, contentRowsAvailable, 1);
        }
        if (count % 2 == 1) {
            return oddLayout(count, areaStartCol, areaWidth, contentRowStart, contentRowsAvailable);
        }
        return twoRows(count, areaStartCol, areaWidth, contentRowStart, contentRowsAvailable);
    }

    private static int[] oneRow(
            int count,
            int areaStart,
            int areaWidth,
            int rowStart,
            int rowsAvail,
            int usedRows
    ) {
        int[] slots = new int[count];
        int firstRow = rowStart + Math.max(0, (rowsAvail - usedRows) / 2);
        int colOffset = areaStart + Math.max(0, (areaWidth - count) / 2);
        for (int i = 0; i < count; i++) {
            slots[i] = firstRow * WIDTH + colOffset + i;
        }
        return slots;
    }

    private static int[] twoRows(int count, int areaStart, int areaWidth, int rowStart, int rowsAvail) {
        int[] slots = new int[count];
        int perRow = count / 2;
        int firstRow = rowStart + Math.max(0, (rowsAvail - 2) / 2);
        int index = 0;
        for (int r = 0; r < 2; r++) {
            int colOffset = areaStart + Math.max(0, (areaWidth - perRow) / 2);
            for (int c = 0; c < perRow; c++) {
                slots[index++] = (firstRow + r) * WIDTH + colOffset + c;
            }
        }
        return slots;
    }

    private static int[] oddLayout(int count, int areaStart, int areaWidth, int rowStart, int rowsAvail) {
        int[] slots = new int[count];
        int rest = count - 1;
        int perRow = rest / 2;
        int firstRow = rowStart + Math.max(0, (rowsAvail - 3) / 2);

        int topCol = areaStart + (areaWidth - 1) / 2;
        slots[0] = firstRow * WIDTH + topCol;

        int index = 1;
        for (int r = 1; r <= 2; r++) {
            int colOffset = areaStart + Math.max(0, (areaWidth - perRow) / 2);
            for (int c = 0; c < perRow; c++) {
                slots[index++] = (firstRow + r) * WIDTH + colOffset + c;
            }
        }
        return slots;
    }
}
