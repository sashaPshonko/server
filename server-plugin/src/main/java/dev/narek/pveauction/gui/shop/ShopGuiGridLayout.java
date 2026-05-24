package dev.narek.pveauction.gui.shop;

/**
 * Единая сетка для всех меню скупки: блок по центру, ряды с одного края.
 */
public final class ShopGuiGridLayout {

    public static final int WIDTH = 9;
    public static final int CONTENT_ROWS = 4;
    public static final int CONTENT_ROW_START = 1;

    private ShopGuiGridLayout() {}

    /** Слоты по порядку (слева направо, сверху вниз). */
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
        int rows = rowsForCount(count);
        int firstRow = contentRowStart + Math.max(0, (contentRowsAvailable - rows) / 2);
        int[] rowSizes = rowSizes(count, rows);
        int maxInRow = 0;
        for (int size : rowSizes) {
            maxInRow = Math.max(maxInRow, size);
        }
        int blockOffset = areaStartCol + Math.max(0, (areaWidth - maxInRow) / 2);

        int[] slots = new int[count];
        int index = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < rowSizes[r]; c++) {
                slots[index++] = (firstRow + r) * WIDTH + blockOffset + c;
            }
        }
        return slots;
    }

    public static int rowsForCount(int count) {
        if (count <= 9) {
            return 1;
        }
        if (count <= 27) {
            return 3;
        }
        return 4;
    }

    private static int[] rowSizes(int count, int rows) {
        int base = count / rows;
        int extra = count % rows;
        int[] sizes = new int[rows];
        for (int r = 0; r < rows; r++) {
            sizes[r] = base + (r < extra ? 1 : 0);
        }
        return sizes;
    }
}
