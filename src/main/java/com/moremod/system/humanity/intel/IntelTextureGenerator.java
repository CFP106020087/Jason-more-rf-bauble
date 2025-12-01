package com.moremod.system.humanity.intel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 情报统计书贴图生成器
 * 运行此类的main方法生成贴图文件
 */
public class IntelTextureGenerator {

    public static void main(String[] args) {
        generateIntelStatisticsBookTexture();
    }

    /**
     * 生成情报统计书贴图 (16x16)
     * 设计：深蓝色书本，带有条形统计图标
     */
    public static void generateIntelStatisticsBookTexture() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        // 颜色定义
        int transparent = 0x00000000;
        int bookCover = 0xFF1A237E;      // 深蓝色封面
        int bookCoverLight = 0xFF3949AB; // 亮蓝色
        int bookSpine = 0xFF0D47A1;      // 书脊
        int paper = 0xFFF5F5DC;          // 纸张米色
        int chartRed = 0xFFE53935;       // 红色柱状
        int chartGreen = 0xFF43A047;     // 绿色柱状
        int chartBlue = 0xFF1E88E5;      // 蓝色柱状
        int gold = 0xFFFFD700;           // 金色装饰

        // 绘制像素
        int[][] pixels = new int[size][size];

        // 初始化为透明
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                pixels[y][x] = transparent;
            }
        }

        // 书本主体 (x: 2-13, y: 1-14)
        for (int y = 1; y <= 14; y++) {
            for (int x = 2; x <= 13; x++) {
                pixels[y][x] = bookCover;
            }
        }

        // 书脊 (左边缘)
        for (int y = 1; y <= 14; y++) {
            pixels[y][2] = bookSpine;
            pixels[y][3] = bookSpine;
        }

        // 亮色边缘
        for (int y = 1; y <= 14; y++) {
            pixels[y][13] = bookCoverLight;
        }

        // 纸张区域 (x: 4-12, y: 3-12)
        for (int y = 3; y <= 12; y++) {
            for (int x = 4; x <= 12; x++) {
                pixels[y][x] = paper;
            }
        }

        // 统计图柱状 (3个柱子)
        // 红色柱 (x: 5-6, y: 8-11)
        for (int y = 8; y <= 11; y++) {
            pixels[y][5] = chartRed;
            pixels[y][6] = chartRed;
        }

        // 绿色柱 (x: 7-8, y: 5-11) - 最高
        for (int y = 5; y <= 11; y++) {
            pixels[y][7] = chartGreen;
            pixels[y][8] = chartGreen;
        }

        // 蓝色柱 (x: 9-10, y: 7-11)
        for (int y = 7; y <= 11; y++) {
            pixels[y][9] = chartBlue;
            pixels[y][10] = chartBlue;
        }

        // 金色装饰线
        for (int x = 4; x <= 12; x++) {
            pixels[2][x] = gold;
            pixels[13][x] = gold;
        }

        // 写入图像
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, pixels[y][x]);
            }
        }

        // 保存文件
        try {
            File outputFile = new File("intel_statistics_book.png");
            ImageIO.write(image, "png", outputFile);
            System.out.println("贴图已生成: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("生成贴图失败: " + e.getMessage());
        }
    }
}
