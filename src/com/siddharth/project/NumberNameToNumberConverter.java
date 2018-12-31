package com.siddharth.project;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberNameToNumberConverter {

   // Define Dictionary for Units, Tens and Scales in arrays in a sequence !important
   final private static String[] DICT_UNITS       = {"zero", "one", "two", "three", "four", "five", "six", "seven",
            "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
            "eighteen", "nineteen"};
   final private static String[] DICT_TENS        = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy",
            "eighty", "ninety"};
   final private static String[] DICT_SCALES      = {"hundred", "thousand", "million", "billion", "trillion"};

   final private static String   TYPE_UNITS       = "UNIT";
   final private static String   TYPE_TENS        = "TEN";
   final private static String   TYPE_SCALES      = "SCALE";
   final private static String   TYPE_NON_NUMERIC = "NON_NUMERIC";
   final private static String   SPACE            = " ";
   final private static String   IS_DIGIT_REGEX   = "[+-]?\\d*?(?:,?\\d+)*\\.?\\d*";

   public static void main (String[] args) {

      String text = "As per a June two thousand seventeen census by the National Dairy Development Board, there are approximately five hundred twelve million one hundred thousand bovines in India.";

      String convertedText = utils.convertNumberNameToNumber(text);

      if ("As per a June 2017 census by the National Dairy Development Board, there are approximately 512100000 bovines in India."
               .equals(convertedText))
         System.out.println("Test Success");
      else
         System.out.println("Test Failed");

   }

   private static NumberNameToNumberConverter utils = new NumberNameToNumberConverter();

   /**
    * @param text
    * @return Converted text with substituted numeric representation.
    * 
    */
   public String convertNumberNameToNumber (String text) {
      if (text == null || text.trim().isEmpty()) {
         return text;
      }
      long startTime = System.currentTimeMillis();
      try {

         text = preprocess(text);

         String[] tokens = text.split("\\s+");
         StringBuilder processedText = new StringBuilder();

         Queue<ValueType> q = new LinkedList<ValueType>();

         for (int i = 0; i < tokens.length; i++) {
            // Find token in Dictionary, and get its type and equivalent numeric value if it was a numeric type
            ValueType tok = findInDictionary(tokens[i]);
            if (tok.getType().equals(TYPE_NON_NUMERIC)) {
               // If non numeric type is reached, then process all the numbers in queue, and append it to string. Keep appending the non numeric tokens.
               if (!q.isEmpty()) {
                  Long processed = process(q);
                  processedText.append(processed + SPACE);
                  // Instead, pretty print in decimal system - query.append((new DecimalFormat("#,###")).format(processed) + SPACE);
               }
               processedText.append(tokens[i] + SPACE);
            } else {
               // If token is of any numeric type, then simply add to queue.
               q.add(tok);
            }
         }
         // Handle non empty queue in the scenario that the last word is a number, and there is no non numeric token after it.
         if (!q.isEmpty()) {
            Long processed = process(q);
            processedText.append(processed);
         }
         System.out.println(
                  "'Number-Names To Number' processing time : " + (System.currentTimeMillis() - startTime) + " ms.\n");
         System.out.println("BEFORE\t- " + text + "\n\nAFTER\t- " + processedText.toString() + "\n");
         return processedText.toString().trim();
      } catch (Exception e) {
         e.printStackTrace();
         return text;
      }
   }

   private String preprocess (String text) {
      return text.replaceAll("[\\-]", SPACE);
   }

   /**
    * Processing is done with the help of a queue and a stack.
    * Queue - Multiply all applicable scales to its own corresponding units, and then add to stack.
    * Stack - If below item is of lower order, then scale it upto the same order as top, and then add it to top.
    */
   private static Long process (Queue<ValueType> q) {
      Stack<Long> stack = new Stack<Long>();
      boolean isLastAScale = false;
      Long partNum = 0L;

      // Evaluate queue and push to stack
      while (q.peek() != null) {
         ValueType valType = q.poll();
         if (valType.getType().equals(TYPE_SCALES)) {
            isLastAScale = true;
            partNum = partNum == 0 ? valType.getValue() : partNum * valType.getValue();
         } else {
            if (isLastAScale) {
               isLastAScale = false;
               stack.add(partNum);
               partNum = 0L;
            }
            partNum += valType.getValue();
         }
      }
      if (partNum != 0L)
         stack.add(partNum);

      // Evaluate stack
      while (stack.size() > 1) {
         Long top = stack.pop();
         if (!stack.isEmpty()) {
            Long next = stack.pop();
            stack.push(next > top ? (next + top) : ((next * getScale(top)) + top));
         }
      }
      return stack.pop();
   }

   private static Long getScale (Long value) {
      int len = value.toString().length();
      if (len >= 1 && len < 4) {
         return 1L;
      } else if (len >= 4 && len < 7) {
         return 1000L;
      } else if (len >= 7 && len < 9) {
         return 1000000L;
      } else if (len >= 9 && len < 11) {
         return 1000000000L;
      } else if (len >= 11 && len < 13) {
         return 1000000000000L;
      } else
         return null;
      // Upper boundary for trillion
   }

   private static ValueType findInDictionary (String word) {
      for (int i = 0; i < DICT_UNITS.length; i++) {
         if (word.equalsIgnoreCase(DICT_UNITS[i])) {
            return utils.new ValueType((long) i, TYPE_UNITS);
         }
      }
      for (int i = 0; i < DICT_TENS.length; i++) {
         if (word.equalsIgnoreCase(DICT_TENS[i])) {
            return utils.new ValueType((long) (i * 10), TYPE_TENS);
         }
      }
      for (int i = 0; i < DICT_SCALES.length; i++) {
         if (word.equalsIgnoreCase(DICT_SCALES[i])) {
            return i != 0 ? utils.new ValueType((long) (Math.pow(10, (i * 3))), TYPE_SCALES)
                     : utils.new ValueType((long) (Math.pow(10, 2)), TYPE_SCALES);
         }
      }
      if (isDigit(word)) {
         Long lWord = Long.parseLong(word);
         return utils.new ValueType(lWord, getScale(lWord) == 1L ? TYPE_UNITS : TYPE_SCALES);
      }
      return utils.new ValueType(TYPE_NON_NUMERIC);
   }

   /*
    * Inner class to create temp objects.
    */
   private class ValueType {

      private Long   value;
      private String type;

      private ValueType (Long value, String type) {
         this.value = value;
         this.type = type;
      }

      private ValueType (String type) {
         this.type = type;
      }

      private Long getValue () {
         return value;
      }

      private String getType () {
         return type;
      }
   }

   private static boolean isDigit (String str) {
      Matcher matcher = Pattern.compile(IS_DIGIT_REGEX).matcher(str);
      return matcher.matches();
   }
}
