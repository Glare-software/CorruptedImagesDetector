package software.glare.cid.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.process.processes.processor.result.BytesProcessResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by fdman on 06.10.2014.
 */
class ResultsTreeBuilder<T extends BytesProcessResult> {
    private final Logger log = LoggerFactory
            .getLogger(ResultsTreeBuilder.class);

    private TreeItemWithMetaInfo fakeRootItemWithMetaInfo;
    private List<TreeItemWithMetaInfo> allItemsWithMetaInfo = new ArrayList<>();
    private final String rootPath;
    private TreeItem<T> rootTreeItem;

    public ResultsTreeBuilder(String rootPath) {
        this.rootPath = StringUtils.removeEnd(rootPath, File.separator) + File.separator;
        fakeRootItemWithMetaInfo = new TreeItemWithMetaInfo("", "");
        fakeRootItemWithMetaInfo.setItem(new TreeItem((T) new BytesProcessResult("No results...")));

    }

    private TreeItemWithMetaInfo findItem(String fullPath) {
        log.trace("try to find Item by path {}", fullPath);
        for (TreeItemWithMetaInfo myTreeItem : allItemsWithMetaInfo) {
            if (myTreeItem.getFullPath().equals(fullPath)) {
                log.trace("found item {}", myTreeItem);
                return myTreeItem;
            }
        }
        log.trace("NOT found item by path {}", fullPath);
        return null;
    }

    private TreeItemWithMetaInfo createItem(String fullPath, String path) {
        log.trace("try to create Item by fullPath {} and path {}", fullPath, path);
        TreeItemWithMetaInfo treeItemWithMetaInfo = new TreeItemWithMetaInfo(fullPath, path);
        TreeItem treeItem = new TreeItem(new BytesProcessResult(fullPath));
        treeItem.setExpanded(true);
        treeItemWithMetaInfo.setItem(treeItem);

        String parentPath = StringUtils.removeEnd(fullPath, path);
        log.trace("find parent started by {}", parentPath);
        TreeItemWithMetaInfo parent = findItem(parentPath);
        if (parent == null) {
            if (StringUtils.isEmpty(parentPath)) {
                log.trace("used fake root as parent ", parentPath);
            } else {
                throw new IllegalStateException("Parent was not found by " + parentPath);
            }
            parent = fakeRootItemWithMetaInfo;
        }
        parent.getItem().getChildren().add(treeItemWithMetaInfo.getItem());
        allItemsWithMetaInfo.add(treeItemWithMetaInfo);
        log.trace("created item {}", treeItemWithMetaInfo);
        return treeItemWithMetaInfo;
    }

    private TreeItemWithMetaInfo findOrCreateItem(String fullPath, String path) {
        log.trace("* try to find or create Item by fullPath {} and path {}", fullPath, path);
        TreeItemWithMetaInfo result = findItem(fullPath);
        if (result == null) {
            result = createItem(fullPath, path);
        }
        return result;
    }

    public TreeItem<T> generateTree(List<T> bytesProcessResults) {
        /*String exception="" ;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()){
            exception += element.toString() + "\n";
        }                       */
        for (BytesProcessResult bytesProcessResult : bytesProcessResults) {
            String fullPath = bytesProcessResult.getPath().toString();
            List<String> paths = Arrays.asList(fullPath.split(Pattern.quote(File.separator)));
            StringBuilder pathBuilder = new StringBuilder("");
            int i = 0;
            for (String path : paths) {
                i = i + 1;
                if (i == paths.size()) {
                    pathBuilder.append(path);
                } else {
                    pathBuilder.append(path).append(File.separator);
                    path = path + File.separator;
                }
                TreeItemWithMetaInfo myTreeItem = findOrCreateItem(pathBuilder.toString(), path);
                if (myTreeItem.getFullPath().equals(fullPath)) {
                    log.trace("bytesProcessResults was set for item with fullPath {} and path {}", fullPath, path);
                    myTreeItem.getItem().setValue(bytesProcessResult);

                    myTreeItem.getItem().setGraphic(new ImageView(
                            "icons/com.iconfinder/tango-icon-library/1415555615_image-x-generic-20.png"));
                } else {
                    myTreeItem.getItem().setGraphic(new ImageView(
                            "icons/com.iconfinder/tango-icon-library/1415555509_folder-open-20.png"));

                }
                tryToDetectRealRoot(myTreeItem);
            }
        }
        TreeItem<T> treeRootItem = rootTreeItem == null ? fakeRootItemWithMetaInfo.getItem() : rootTreeItem;
        return treeRootItem;
    }

    private void tryToDetectRealRoot(TreeItemWithMetaInfo currentChild) {
        if (rootTreeItem == null && currentChild.getFullPath().equals(rootPath)) {
            rootTreeItem = currentChild.getItem();
        }
    }
}

class TreeItemWithMetaInfo<T> {
    private final String fullPath;
    private final String currentPath;
    private TreeItem<? extends T> item;

    TreeItemWithMetaInfo(String fullPath, String currentPath) {
        this.fullPath = fullPath;
        this.currentPath = currentPath;
    }

    public void setItem(TreeItem<T> item) {
        this.item = item;
    }

    public TreeItem<?> getItem() {
        return item;
    }

    public String getFullPath() {
        return fullPath;
    }

    @Override
    public String toString() {
        return "TreeItemWithMetaInfo{" +
                "fullPath='" + fullPath + '\'' +
                ", currentPath='" + currentPath + '\'' +
                ", itemvalue=" + item.getValue() +
                '}';
    }
}
