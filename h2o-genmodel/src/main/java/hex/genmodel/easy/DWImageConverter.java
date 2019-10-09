package hex.genmodel.easy;

import hex.genmodel.GenModel;
import hex.genmodel.algos.deepwater.DeepwaterMojoModel;
import hex.genmodel.easy.exception.PredictException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class DWImageConverter extends RowToRawDataConverter {

  private final DeepwaterMojoModel _dwm;
  
  DWImageConverter(DeepwaterMojoModel m, HashMap<String, Integer> modelColumnNameToIndexMap, HashMap<Integer, HashMap<String, Integer>> domainMap,
                          EasyPredictModelWrapper.ErrorConsumer errorConsumer, EasyPredictModelWrapper.Config config) {
    super(m, modelColumnNameToIndexMap, domainMap, errorConsumer, config);
    _dwm = m;
  }

  @Override
  protected boolean convertValue(String columnName, Object o, String[] domainValues, int targetIndex, double[] rawData) throws PredictException {
    BufferedImage img = null;

    if (o instanceof String) {
      String s = ((String) o).trim();
      // Url to an image given
      boolean isURL = s.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
      try {
        img = isURL ? ImageIO.read(new URL(s)) : ImageIO.read(new File(s));
      } catch (IOException e) {
        throw new PredictException("Couldn't read image from " + s);
      }
    } else if (o instanceof byte[]) {
      // Read the image from raw bytes
      InputStream is = new ByteArrayInputStream((byte[]) o);
      try {
        img = ImageIO.read(is);
      } catch (IOException e) {
        throw new PredictException("Couldn't interpret raw bytes as an image.");
      }
    }

    if (img != null) {
      int W = _dwm._width;
      int H = _dwm._height;
      int C = _dwm._channels;
      float[] _destData = new float[W * H * C];
      try {
        GenModel.img2pixels(img, W, H, C, _destData, 0, _dwm._meanImageData);
      } catch (IOException e) {
        e.printStackTrace();
        throw new PredictException("Couldn't vectorize image.");
      }
      rawData = new double[_destData.length];
      for (int i = 0; i < rawData.length; ++i)
        rawData[i] = _destData[i];
      return true;
    } else
      return super.convertValue(columnName, o, domainValues, targetIndex, rawData);
  }

}