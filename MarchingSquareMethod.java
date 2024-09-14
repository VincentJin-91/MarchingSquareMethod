package com.vincent.demo;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.ArrayList;

public class MarchingSquareMethod {

    public interface PixelInsideHandler {
        boolean isInside(@ColorInt int pixelColor);
    }

    public static class ContourBean {
        public ArrayList<Point> points = new ArrayList<>();

        public Rect bitmapBound = new Rect();

        public Rect bound = new Rect();

        public Path path = new Path();
      
        public Region region = new Region();
    }

    public static ArrayList<ContourBean> getContours(Bitmap bitmap, int color) {
        return getContours(bitmap, new PixelInsideHandler() {
            @Override
            public boolean isInside(int pixelColor) {
                return pixelColor != color;
            }
        });
    }

    public static ArrayList<ContourBean> getContours(@NonNull Bitmap bitmap, @NonNull PixelInsideHandler pixelInsideHandler) {
        ArrayList<ContourBean> result = new ArrayList<>();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] data = new int[width * height];
        bitmap.getPixels(data, 0, width, 0, 0, width, height);
        int startPos = -1;
        //找第一个非color颜色的像素
        for (int i = 0; i < data.length; i++) {
            if (pixelInsideHandler.isInside(data[i])) {
                startPos = i;
                break;
            }
        }
        outer:
        while (true) {
            if (startPos == width * height - 1) {
                break outer;
            }
            inner:
            for (ContourBean preBean : result) {
                if (preBean.points.size() <= 1) {
                    continue inner;
                }
                if (preBean.region.contains(startPos % width, startPos / width)) {
                    startPos++;
                    if (!pixelInsideHandler.isInside(data[startPos])) {
                        thirInner:
                        for (int i = startPos; i < data.length; i++) {
                            if (pixelInsideHandler.isInside(data[i])) {
                                startPos = i;
                                break thirInner;
                            }
                        }
                    }
                    continue outer;
                }
            }
            ContourBean contours = null;
            if (startPos >= 0) {
                contours = traceRegion(startPos % width, startPos / width, width, height, data, pixelInsideHandler);
                result.add(contours);
            }
            //接着找第一个非color颜色的像素
            startPos++;
            secInner:
            for (int i = startPos; i < data.length; i++) {
                if (pixelInsideHandler.isInside(data[i])) {
                    startPos = i;
                    break secInner;
                }
            }

        }

        return result;
    }

    /**
     * @module marching-squares
     */

// [dx, dy, is-down-or-right?0:1]
    private static final int[] UP = new int[]{0, -1, 1};
    private static final int[] DOWN = new int[]{0, 1, 0};
    private static final int[] LEFT = new int[]{-1, 0, 1};
    private static final int[] RIGHT = new int[]{1, 0, 0};

    private static final int[][][] transitions = new int[][][]{
            null, // 0
            // [direction-if-coming-from-down-or-right, direction-if-coming-from-up-or-left]
            new int[][]{LEFT, LEFT}, // 1
            new int[][]{UP, UP}, // 2
            new int[][]{LEFT, LEFT}, // 3
            new int[][]{DOWN, DOWN},  // 4
            new int[][]{DOWN, DOWN},  // 5
            new int[][]{UP, DOWN},   // 6
            new int[][]{DOWN, DOWN}, // 7
            new int[][]{RIGHT, RIGHT}, // 8
            new int[][]{RIGHT, LEFT}, // 9
            new int[][]{UP, UP}, // 10
            new int[][]{LEFT, LEFT}, // 11
            new int[][]{RIGHT, RIGHT}, // 12
            new int[][]{RIGHT, RIGHT}, // 13
            new int[][]{UP, UP} // 14
    };

    private static ContourBean traceRegion(int x, int y, int width, int height, @NonNull int[] pixels, @NonNull PixelInsideHandler pixelInsideHandler) {
        int startX = x, startY = y;
        ContourBean contourBean = new ContourBean();
        contourBean.bitmapBound.set(0, 0, width, height);
        ArrayList<Point> ret = new ArrayList<>();
        contourBean.points = ret;
        contourBean.path.reset();
        contourBean.path.moveTo(x, y);
        ret.add(new Point(x, y));
        contourBean.bound.left = x;
        contourBean.bound.top = y;
        contourBean.bound.right = x;
        contourBean.bound.bottom = y;
        int[] dir = DOWN; // arbitrary

        int square =
                (isInside(x - 1, y - 1, width, height, pixels, pixelInsideHandler) ? 1 : 0) +
                        (isInside(x, y - 1, width, height, pixels, pixelInsideHandler) ? 2 : 0) +
                        (isInside(x - 1, y, width, height, pixels, pixelInsideHandler) ? 4 : 0) +
                        (isInside(x, y, width, height, pixels, pixelInsideHandler) ? 8 : 0);

        if (square == 0 || square == 15)
            return contourBean;
//            throw new Error("Bad Starting point.");

        while (true) {
            dir = transitions[square][dir[2]];
            x += dir[0];
            y += dir[1];

            if (x == startX && y == startY) {
                contourBean.path.close();
                contourBean.region.setEmpty();
                contourBean.region.setPath(contourBean.path, new Region(contourBean.bitmapBound));
                return contourBean;
            }

            if (x < contourBean.bound.left) {
                contourBean.bound.left = x;
            }
            if (x > contourBean.bound.right) {
                contourBean.bound.right = x;
            }
            if (y < contourBean.bound.top) {
                contourBean.bound.top = y;
            }
            if (y > contourBean.bound.bottom) {
                contourBean.bound.bottom = y;
            }
            contourBean.path.lineTo(x, y);
            ret.add(new Point(x, y));

            if (dir == DOWN)
                square = ((square & 12) >> 2);
            else if (dir == UP)
                square = ((square & 3) << 2);
            else if (dir == RIGHT)
                square = ((square & 10) >> 1);
            else if (dir == LEFT)
                square = ((square & 5) << 1);

            if (dir == DOWN || dir == LEFT)
                square += (isInside(x - 1, y, width, height, pixels, pixelInsideHandler) ? 4 : 0);
            else
                square += (isInside(x, y - 1, width, height, pixels, pixelInsideHandler) ? 2 : 0);

            if (dir == DOWN || dir == RIGHT)
                square += (isInside(x, y, width, height, pixels, pixelInsideHandler) ? 8 : 0);
            else
                square += (isInside(x - 1, y - 1, width, height, pixels, pixelInsideHandler) ? 1 : 0);
        }

    }

    private static boolean isInside(int x, int y, int width, int height, @NonNull int[] data, @NonNull PixelInsideHandler pixelInsideHandler) {
        return x >= 0 && y >= 0 && x < width && y < height && pixelInsideHandler.isInside(data[(y * width + x)]);
    }

}
