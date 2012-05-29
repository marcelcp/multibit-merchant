package org.multibit.mbm.qrcode;

/**
 * Uses code from com.google.zxing.qrcode.QRCodeWriter which is:
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

/**
 * <p>Utility class for to create swatches</p>
 * <ul>
 * <li>Create swatch from bitcoin uri</li>
 * <li>Parse bitcoin uri to components</li>
 * </p>
 *
 * @since 0.0.1
 */
// TODO Reinstate these annotations
//@Component
//@Singleton
public class SwatchGenerator {

  // these constants should migrate into a swatch model that is generated by a
  // swatch designer

  // large
  // private static final int QUIET_ZONE_SIZE = 16;
  // private static final int WIDTH_OF_TEXT_BORDER = 8;
  // private static final int LEFT_TEXT_INSET = 12;
  // private static final int RIGHT_TEXT_INSET = 12;
  // private static final int BOTTOM_TEXT_INSET = 16;
  // private static final int TOP_TEXT_INSET = 8;
  // private static final int GAP_BETWEEN_TEXT_ROWS = 8;
  // private static final int GAP_ABOVE_ADDRESS = 16;
  // private static int BOUNDING_BOX_PADDING = 8;
  // private static int QR_CODE_ELEMENT_MULTIPLE = 8;

  // small
  private static final int QUIET_ZONE_SIZE = 4;
  private static final int WIDTH_OF_TEXT_BORDER = 2;
  private static final int LEFT_TEXT_INSET = 3;
  private static final int RIGHT_TEXT_INSET = 3;
  private static final int BOTTOM_TEXT_INSET = 4;
  private static final int TOP_TEXT_INSET = 2;
  private static final int GAP_BETWEEN_TEXT_ROWS = 2;
  private static final int GAP_ABOVE_ADDRESS = 4;
  private static int BOUNDING_BOX_PADDING = 2;
  private static int QR_CODE_ELEMENT_MULTIPLE = 2;

  private static Font addressFont=new Font("Verdana", Font.PLAIN, 10);
  private static Font labelFont=new Font("Times New Roman", Font.PLAIN, 17);
  private static Font amountFont=labelFont;
  private static Map<Font, FontMetrics> fontToFontMetricsMap;

  private BufferedImage emptyImage;
  private Graphics2D emptyGraphics;
  private QRCode code;

  public SwatchGenerator() {
    // graphics context - used to work out the width of the swatch
    emptyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    emptyGraphics = emptyImage.createGraphics();

    code = new QRCode();

    // fonts
    addressFont = new Font("Verdana", Font.PLAIN, 10); // 10 best
    labelFont = new  Font("Serif", Font.PLAIN, 17); //new Font("Times New Roman", Font.PLAIN, 17); // 17 looks best
    amountFont = labelFont;

    // cached to save time
    fontToFontMetricsMap = new HashMap<Font, FontMetrics>();
    fontToFontMetricsMap.put(addressFont, emptyGraphics.getFontMetrics(addressFont));
    fontToFontMetricsMap.put(labelFont, emptyGraphics.getFontMetrics(labelFont));
    fontToFontMetricsMap.put(amountFont, emptyGraphics.getFontMetrics(amountFont));

    // initialise statics for extra speed later
    BitcoinURI.convertToBitcoinURI("1", "1", "1");
    
    // make sure fonts are loaded
    JFrame frame = new JFrame();
  }

  /**
   * generate a Swatch
   * 
   * @param address
   *          Bitcoin address to show
   * @param amount
   *          amount of BTC to show - text
   * @param label
   *          label for swatch
   * @return
   */
  public BufferedImage generateSwatch(String address, String amount, String label) {
    // long time0 = (new Date()).getTime();
    String bitcoinURI = BitcoinURI.convertToBitcoinURI(address, amount, label);

    // get a byte matrix for the data
    ByteMatrix matrix;
    try {
      matrix = encode(bitcoinURI);
    } catch (com.google.zxing.WriterException e) {
      // exit the method
      return null;
    } catch (IllegalArgumentException e) {
      // exit the method
      return null;
    }

    boolean addAmount;
    if (amount == null || "".equals(amount)) {
      addAmount = false;
    } else {
      addAmount = true;
    }

    if (label == null) {
      label = "";
    }

    // generate an image from the byte matrix
    int matrixWidth = matrix.getWidth();
    int matrixHeight = matrix.getHeight();

    int addressAdvance = getAdvance(emptyGraphics, address, addressFont);
    int amountAdvance = 0;
    if (addAmount) {
      amountAdvance = getAdvance(emptyGraphics, amount + " BTC", amountFont);
    }
    // convert backslash-rs to backslash-ns
    label = label.replaceAll("\r\n", "\n");
    label = label.replaceAll("\n\r", "\n");
    String[] labelLines = label.split("[\\n\\r]");

    int maxLabelAdvance = 0;

    if (labelLines != null) {
      for (int i = 0; i < labelLines.length; i++) {
        int labelAdvance = getAdvance(emptyGraphics, labelLines[i], labelFont);
        if (labelAdvance > maxLabelAdvance) {
          maxLabelAdvance = labelAdvance;
        }
      }
    }

    int widestTextAdvance = (int) Math.max(Math.max(addressAdvance, amountAdvance), maxLabelAdvance);
    int swatchWidth = matrixWidth + widestTextAdvance + LEFT_TEXT_INSET + RIGHT_TEXT_INSET + WIDTH_OF_TEXT_BORDER * 2
        + QUIET_ZONE_SIZE;

    // work out the height of the swatch
    int minimumBoxHeight = TOP_TEXT_INSET + BOTTOM_TEXT_INSET + 2 * (QUIET_ZONE_SIZE + WIDTH_OF_TEXT_BORDER) + GAP_ABOVE_ADDRESS
        + addressFont.getSize();
    if (addAmount) {
      minimumBoxHeight = minimumBoxHeight + GAP_BETWEEN_TEXT_ROWS + amountFont.getSize();
    }

    if (labelLines != null) {
      minimumBoxHeight = minimumBoxHeight + labelLines.length * (labelFont.getSize() + GAP_BETWEEN_TEXT_ROWS - 1);
    }

    int swatchHeight;
    if (minimumBoxHeight > matrixHeight) {
      swatchHeight = minimumBoxHeight;
    } else {
      swatchHeight = matrixHeight;
    }

    // create buffered image to draw to
    BufferedImage image = new BufferedImage(swatchWidth, swatchHeight, BufferedImage.TYPE_INT_RGB);

    // iterate through the matrix and draw the pixels to the image
    int qrCodeVerticalOffset = 0;
    if (swatchHeight > matrixHeight) {
      qrCodeVerticalOffset = (int) ((swatchHeight - matrixHeight) * 0.5);
    }
    for (int y = 0; y < matrixHeight; y++) {
      for (int x = 0; x < matrixWidth; x++) {
        byte imageValue = matrix.get(x, y);
        image.setRGB(x, y + qrCodeVerticalOffset, imageValue);
      }
    }

    // fill in the rest of the image as white
    for (int y = 0; y < swatchHeight; y++) {
      for (int x = matrixWidth; x < swatchWidth; x++) {
        image.setRGB(x, y, 0xFFFFFF);
      }
    }
    if (swatchHeight > matrixHeight) {
      for (int y = 0; y < qrCodeVerticalOffset; y++) {
        for (int x = 0; x < swatchWidth; x++) {
          image.setRGB(x, y, 0xFFFFFF);
        }
      }

      for (int y = matrixHeight + qrCodeVerticalOffset; y < swatchHeight; y++) {
        for (int x = 0; x < swatchWidth; x++) {
          image.setRGB(x, y, 0xFFFFFF);
        }
      }
    }

    // draw the text box
    for (int y = QUIET_ZONE_SIZE; y < swatchHeight - QUIET_ZONE_SIZE; y++) {
      for (int loopX = 0; loopX < WIDTH_OF_TEXT_BORDER; loopX++) {
        // left hand side
        image.setRGB(matrixWidth + loopX, y, 0x000000);

        // right hand side
        image.setRGB(swatchWidth - QUIET_ZONE_SIZE - loopX - 1, y, 0x000000);
      }
    }

    for (int x = matrixWidth + QUIET_ZONE_SIZE - WIDTH_OF_TEXT_BORDER; x < swatchWidth - QUIET_ZONE_SIZE; x++) {
      for (int loopY = 0; loopY < WIDTH_OF_TEXT_BORDER; loopY++) {
        // top side
        image.setRGB(x, QUIET_ZONE_SIZE + loopY, 0x000000);

        // bottom side
        image.setRGB(x, swatchHeight - QUIET_ZONE_SIZE - loopY - 1, 0x000000);
      }
    }

    Graphics2D g2 = image.createGraphics();

    g2.setColor(Color.black);
    g2.setFont(addressFont);
    // left justified
    // g2.drawString(address, matrixWidth + QUIET_ZONE_SIZE +
    // WIDTH_OF_TEXT_BORDER, swatchHeight - QUIET_ZONE_SIZE -
    // WIDTH_OF_TEXT_BORDER
    // - BOTTOM_TEXT_INSET);

    // right justified
    g2.drawString(address, swatchWidth - QUIET_ZONE_SIZE - WIDTH_OF_TEXT_BORDER - RIGHT_TEXT_INSET - addressAdvance, swatchHeight
        - QUIET_ZONE_SIZE - WIDTH_OF_TEXT_BORDER - BOTTOM_TEXT_INSET);

    g2.setFont(labelFont);
    if (labelLines != null) {
      for (int i = 0; i < labelLines.length; i++) {
        g2.drawString(labelLines[i], matrixWidth + QUIET_ZONE_SIZE + WIDTH_OF_TEXT_BORDER, QUIET_ZONE_SIZE + TOP_TEXT_INSET
            + labelFont.getSize() + i * (labelFont.getSize() + GAP_BETWEEN_TEXT_ROWS));
      }
      if (addAmount) {
        g2.setFont(amountFont);
        // left justified
        // g2.drawString(amount + " BTC", matrixWidth + QUIET_ZONE_SIZE +
        // WIDTH_OF_TEXT_BORDER, QUIET_ZONE_SIZE
        // + TOP_TEXT_INSET + labelLines.length * (labelFont.getSize() +
        // GAP_BETWEEN_TEXT_ROWS) + amountFont.getSize());

        // bottom right justified
        g2.drawString(amount + " BTC", swatchWidth - QUIET_ZONE_SIZE - WIDTH_OF_TEXT_BORDER - RIGHT_TEXT_INSET - amountAdvance,
            swatchHeight - QUIET_ZONE_SIZE - WIDTH_OF_TEXT_BORDER - BOTTOM_TEXT_INSET - addressFont.getSize() - GAP_ABOVE_ADDRESS);
      }
    } else {
      if (addAmount) {
        g2.setFont(amountFont);
        g2.drawString(amount + " BTC", matrixWidth + QUIET_ZONE_SIZE + WIDTH_OF_TEXT_BORDER, QUIET_ZONE_SIZE + TOP_TEXT_INSET
            + amountFont.getSize());
      }
    }
    // long time1 = (new Date()).getTime();
    // System.out.println("SwatchGenerator#generateSwatch took " + (time1 -
    // time0) + " millisec");
    return image;
  }

  private int getAdvance(Graphics graphics, String text, Font font) {
    // get metrics from the graphics
    FontMetrics metrics = fontToFontMetricsMap.get(font);

    // get the advance of my text in this font and render context
    int advance = metrics.stringWidth(text);

    return advance + BOUNDING_BOX_PADDING;
  }

  public static void main(String[] args) {
    SwatchGenerator swatchGenerator = new SwatchGenerator();
    String address = "15BGmyMKxGFkejW1oyf2Gwv3NHqeUP7aWh";
    String amount = "0.423232";
    String label = "A longish label xyz";

    BufferedImage swatch = swatchGenerator.generateSwatch(address, amount, label);
    ImageIcon icon = new ImageIcon(swatch);
    JOptionPane.showMessageDialog(null, "", "Swatch Generator 1", JOptionPane.INFORMATION_MESSAGE, icon);

    address = "1HB5XMLmzFVj8ALj6mfBsbifRoD4miY36v";
    amount = "0.5";
    label = "Donate to Wikileaks\nWith a second line";

    swatch = swatchGenerator.generateSwatch(address, amount, label);
    icon = new ImageIcon(swatch);
    JOptionPane.showMessageDialog(null, "", "Swatch Generator 2", JOptionPane.INFORMATION_MESSAGE, icon);

    address = "15BGmyMKxGFkejW1oyf2Gwv3NHqeUP7aWh";
    amount = "0.41";
    label = "";

    swatch = swatchGenerator.generateSwatch(address, amount, label);
    icon = new ImageIcon(swatch);
    JOptionPane.showMessageDialog(null, "", "Swatch Generator 3", JOptionPane.INFORMATION_MESSAGE, icon);

    address = "15BGmyMKxGFkejW1oyf2Gwv3NHqeUP7aWh";
    amount = "";
    label = "A longerer label xyzabc - with no amount";

    swatch = swatchGenerator.generateSwatch(address, amount, label);
    icon = new ImageIcon(swatch);
    JOptionPane.showMessageDialog(null, "", "Swatch Generator 4", JOptionPane.INFORMATION_MESSAGE, icon);

    address = "15BGmyMKxGFkejW1oyf2Gwv3NHqeUP7aWh";
    amount = "1.2";
    label = "Shorty\r\non three\rseparate lines";

    swatch = swatchGenerator.generateSwatch(address, amount, label);
    icon = new ImageIcon(swatch);
    JOptionPane.showMessageDialog(null, "", "Swatch Generator 5", JOptionPane.INFORMATION_MESSAGE, icon);

    // write the image to the output stream
    try {
      ImageIO.write(swatch, "png", new File("swatch.png"));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * This object renders a QR Code as a ByteMatrix 2D array of greyscale values.
   * 
   * @author dswitkin@google.com (Daniel Switkin)
   */
  public ByteMatrix encode(String contents) throws WriterException {

    if (contents == null || contents.length() == 0) {
      throw new IllegalArgumentException("Found empty contents");
    }

    Encoder.encode(contents, ErrorCorrectionLevel.L, null, code);
    return renderResult(code, QR_CODE_ELEMENT_MULTIPLE);
  }

  // Note that the input matrix uses 0 == white, 1 == black, while the output
  // matrix uses
  // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
  private static ByteMatrix renderResult(QRCode code, int multiple) {
    ByteMatrix input = code.getMatrix();
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int qrWidth = multiple * inputWidth + (QUIET_ZONE_SIZE << 1);
    int qrHeight = multiple * inputHeight + (QUIET_ZONE_SIZE << 1);

    ByteMatrix output = new ByteMatrix(qrWidth, qrHeight);
    byte[][] outputArray = output.getArray();

    // We could be tricky and use the first row in each set of multiple as
    // the temporary storage,
    // instead of allocating this separate array.
    byte[] row = new byte[qrWidth];

    // 1. Write the white lines at the top
    for (int y = 0; y < QUIET_ZONE_SIZE; y++) {
      setRowColor(outputArray[y], (byte) 255);
    }

    // 2. Expand the QR image to the multiple
    byte[][] inputArray = input.getArray();
    for (int y = 0; y < inputHeight; y++) {
      // a. Write the white pixels at the left of each row
      for (int x = 0; x < QUIET_ZONE_SIZE; x++) {
        row[x] = (byte) 255;
      }

      // b. Write the contents of this row of the barcode
      int offset = QUIET_ZONE_SIZE;
      for (int x = 0; x < inputWidth; x++) {
        byte value = (inputArray[y][x] == 1) ? 0 : (byte) 255;
        for (int z = 0; z < multiple; z++) {
          row[offset + z] = value;
        }
        offset += multiple;
      }

      // c. Write the white pixels at the right of each row
      offset = QUIET_ZONE_SIZE + (inputWidth * multiple);
      for (int x = offset; x < qrWidth; x++) {
        row[x] = (byte) 255;
      }

      // d. Write the completed row multiple times
      offset = QUIET_ZONE_SIZE + (y * multiple);
      for (int z = 0; z < multiple; z++) {
        System.arraycopy(row, 0, outputArray[offset + z], 0, qrWidth);
      }
    }

    // 3. Write the white lines at the bottom
    int offset = QUIET_ZONE_SIZE + (inputHeight * multiple);
    for (int y = offset; y < qrHeight; y++) {
      setRowColor(outputArray[y], (byte) 255);
    }

    return output;
  }

  private static void setRowColor(byte[] row, byte value) {
    for (int x = 0; x < row.length; x++) {
      row[x] = value;
    }
  }
  
//  private static final Font SERIF_FONT = new Font("serif", Font.PLAIN, 24);
//
//  private static Font getFont(String name) {
//      Font font = null;
//      if (name == null) {
//          return SERIF_FONT;
//      }
//
//      try {
//          // load from a cache map, if exists
//          if (fonts != null && (font = fonts.get(name)) != null) {
//              return font;
//          }
//          String fName = Params.get().getFontPath() + name;
//          File fontFile = new File(fName);
//          font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
//          GraphicsEnvironment ge = GraphicsEnvironment
//                  .getLocalGraphicsEnvironment();
//
//          ge.registerFont(font);
//
//          fonts.put(name, font);
//      } catch (Exception ex) {
//          log.info(name + " not loaded.  Using serif font.");
//          font = SERIF_FONT;
//      }
//      return font;
//  }

}
