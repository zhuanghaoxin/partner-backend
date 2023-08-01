package cn.ichensw.partner.utils;

import cn.ichensw.partner.enums.ListTypeEnum;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.ichensw.partner.enums.ListTypeEnum.ENGLISH;
import static cn.ichensw.partner.enums.ListTypeEnum.MIXEECAE;

/**
 * 算法工具类
 *
 * @author zhx
 */
public class AlgorithmUtils {

    /**
     * 编辑距离算法（用于计算最相似的两组标签）
     * 原理：https://blog.csdn.net/DBC_121/article/details/104198838
     * @param tagList1
     * @param tagList2
     * @return
     */
    public static int minDistance(List<String> tagList1, List<String> tagList2) {
        //获取两个列表的长度
        int n = tagList1.size();
        int m = tagList2.size();


        // 如果其中有一个为空，说明得经过 n 或者 m 次才能得到最小编辑距离
        if (n * m == 0) {
            return n + m;
        }
        // 动态规划
        // 先将 上边 和 左边 进行赋值，便于后续动态规划
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }

        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }


        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                //
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int left_down = d[i - 1][j - 1];
                if (!Objects.equals(tagList1.get(i - 1), tagList2.get(j - 1))) {
                    left_down += 1;
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }


    public static double sorce(List<String> list1, List<String> list2) throws IOException {
        List<String> resultList1 = list1.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<String> resultList2 = list2.stream().map(String::toLowerCase).collect(Collectors.toList());
        int strType = AllUtils.getStrType(resultList1);
        int type = AllUtils.getStrType(resultList2);
        ListTypeEnum enumByValue = ListTypeEnum.getEnumByValue(strType);
        ListTypeEnum enumByValue1 = ListTypeEnum.getEnumByValue(type);
        if (enumByValue == MIXEECAE) {
            resultList1 = AllUtils.tokenize(resultList1);
        }
        if (enumByValue1 == MIXEECAE) {
            resultList2 = AllUtils.tokenize(resultList2);
        }
        double ikSorce = 0;
        if (enumByValue != ENGLISH && enumByValue1 != ENGLISH) {
            List<String> resultList3 = list1.stream().map(String::toLowerCase).collect(Collectors.toList());
            List<String> resultList4 = list2.stream().map(String::toLowerCase).collect(Collectors.toList());
            List<String> quotedList1 = resultList3.stream()
                    .map(str -> "\"" + str + "\"")
                    .collect(Collectors.toList());
            List<String> quotedList2 = resultList4.stream()
                    .map(str -> "\"" + str + "\"")
                    .collect(Collectors.toList());
            String tags1 = AllUtils.collectChineseChars(quotedList1);
            List<String> Ls = AllUtils.analyzeText(tags1);
            String tags2 = AllUtils.collectChineseChars(quotedList2);
            List<String> Ls2 = AllUtils.analyzeText(tags2);
            ikSorce = AllUtils.calculateJaccardSimilarity(Ls, Ls2);
        }
        int EditDistanceSorce = AllUtils.calculateEditDistance(resultList1, resultList2);
        double maxEditDistance = Math.max(resultList1.size(), resultList2.size());
        double EditDistance = 1 - EditDistanceSorce / maxEditDistance;
        double JaccardSorce = AllUtils.calculateJaccardSimilarity(resultList1, resultList2);
        double similaritySorce = AllUtils.cosineSimilarity(resultList1, resultList2);
        /**
         * 编辑距离 权重为0.5
         * Jaccard相似度算法（ik分词后使用Jaccard相似度算法） 权重为0.3
         *  余弦相似度 权重为0.2
         *
         */
        double totalSorce = EditDistance * 0.5 + JaccardSorce * 0.3 + similaritySorce * 0.2 + ikSorce * 0.3;
        return totalSorce;
    }

    /**
     * 编辑距离算法（用于计算最相似的两个字符串）
     * 原理：https://blog.csdn.net/DBC_121/article/details/104198838
     *
     * @param word1
     * @param word2
     * @return
     */
    public static int minDistance(String word1, String word2) {
        int n = word1.length();
        int m = word2.length();

        if (n * m == 0) {
            return n + m;
        }

        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }

        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int left_down = d[i - 1][j - 1];
                if (word1.charAt(i - 1) != word2.charAt(j - 1)) {
                    left_down += 1;
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }
}
