package dev.narek.pveauction.gui.shop;

/**
 * Симметрия в меню сдачи (как у мясника по вертикали):
 * ≤5 — один ряд; 6+ чётное — два ряда поровну; 6+ нечётное — 1 сверху по центру + два ряда 50/50.
 */
public final class ShopGuiGridLayout {

    public static final int WIDTH = 9;
    public static final int CONTENT_ROWS = 4;
    public static final int CONTENT_ROW_START = 1;

    private ShopGuiGridLayout() {}

    public static int blockRows(int count) {
        if (count <= 5) {
            return 1;
        }
        if (count % 2 == 1) {
            return 3;
        }
        return 2;
    }

    public static int[] slotsForCount(int count) {
        int rows = blockRows(count);
        int firstRow = CONTENT_ROW_START + Math.max(0, (CONTENT_ROWS - rows) / 2);
        return slotsForCount(count, 0, WIDTH, firstRow, rows);
    }

    public static int[] slotsForCount(
            int count,
            int areaStartCol,
            int areaWidth,
            int contentRowStart,
            int contentRowsUsed
    ) {
        if (count <= 0) {
            return new int[0];
        }
        if (count <= 5) {
            return oneRow(count, areaStartCol, areaWidth, contentRowStart, contentRowsUsed);
        }
        if (count % 2 == 1) {
            return oddLayout(count, areaStartCol, areaWidth, contentRowStart, contentRowsUsed);
        }
        return twoRows(count, areaStartCol, areaWidth, contentRowStart, contentRowsUsed);
    }

    private static int[] oneRow(int count, int areaStart, int areaWidth, int rowStart, int rowsUsed) {
        int[] slots = new int[count];
        int row = rowStart + Math.max(0, (rowsUsed - 1) / 2);
        int colOffset = areaStart + Math.max(0, (areaWidth - count) / 2);
        for (int i = 0; i < count; i++) {
            slots[i] = row * WIDTH + colOffset + i;
        }
        return slots;
    }

    private static int[] twoRows(int count, int areaStart, int areaWidth, int rowStart, int rowsUsed) {
        int[] slots = new int[count];
        int perRow = count / 2;
        int firstRow = rowStart + Math.max(0, (rowsUsed - 2) / 2);
        int index = 0;
        for (int r = 0; r < 2; r++) {
            int colOffset = areaStart + Math.max(0, (areaWidth - perRow) / 2);
            for (int c = 0; c < perRow; c++) {
                slots[index++] = (firstRow + r) * WIDTH + colOffset + c;
            }
        }
        return slots;
    }

    private static int[] oddLayout(int count, int areaStart, int areaWidth, int rowStart, int rowsUsed) {
        int[] slots = new int[count];
        int perRow = (count - 1) / 2;
        int firstRow = rowStart + Math.max(0, (rowsUsed - 3) / 2);

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
