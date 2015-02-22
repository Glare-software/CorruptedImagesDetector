package software.glare.cid.ui;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fdman on 09.02.2015.
 */
public class Test {
    public static void main(String[] args) throws IOException, URISyntaxException {
        Test t = new Test();
        t.testFreemarker();
    }

    private void testFreemarker() throws URISyntaxException, IOException {
        Configuration freeMarkerCfg = new Configuration(Configuration.VERSION_2_3_21);
        //freeMarkerCfg.setDirectoryForTemplateLoading(new File(this.getClass().getClassLoader().getResource("freemarker").toURI()));
        freeMarkerCfg.setClassForTemplateLoading(getClass(), "/freemarker/");
        freeMarkerCfg.setDefaultEncoding("UTF-8");

        freeMarkerCfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);

        Template template;
        try {
            template = freeMarkerCfg.getTemplate("licensesThirdParty.ftl");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("testValue", UIConstants.MAIN_TITLE);
        processFreeMarkerTemplate(template,
                "tst.txt", data);


    }

    private void processFreeMarkerTemplate(Template template, String fileName, Map<String, Object> data) {
        try (Writer fileWriter = new FileWriter(new File(fileName));) {
            template.process(data, fileWriter);
            fileWriter.flush();
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
    }
}
