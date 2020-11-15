package shrbox.github.acgpro;

import com.google.gson.Gson;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageUtils;

import java.net.URL;
import java.util.List;
import java.util.Random;

public class Thread extends java.lang.Thread {
    GroupMessageEvent e;

    public void boot(GroupMessageEvent event) {
        this.e = event;
        this.start();
    }

    @Override
    public void run() {
        String msgcontent = e.getMessage().contentToString().toLowerCase().replace("acg", "").trim();
        short picnum = 1;
        if (msgcontent.contains(" ")) {
            String[] str = msgcontent.split(" ", 2);
            msgcontent = str[0];
            if (str[1] == null) return;
            char[] picnum_ = str[1].toCharArray();
            if (!Character.isDigit(picnum_[0])) return;
            picnum = Short.parseShort(str[1]);
        }
        List<Long> r18_groups = Main.config.getLongList("r18-groups");
        boolean isr18 = false;
        if (Main.config.getBoolean("r18") && r18_groups.contains(e.getGroup().getId())) isr18 = true;
        String content = Connection.getURL(msgcontent, isr18);
        if (content.equals("")) {
            e.getGroup().sendMessage("无法访问到接口");
            return;
        }
        Gson gson = new Gson();
        Json_pre json_pre = gson.fromJson(content, Json_pre.class);
        String error_msg = null;
        if (json_pre.code != 0) {
            switch (json_pre.code) {
                case -1:
                    error_msg = "内部错误";
                    break;
                case 401:
                    error_msg = "apikey不存在或被封禁";
                    break;
                case 403:
                    error_msg = "由于不规范操作拒绝调用";
                    break;
                case 404:
                    error_msg = "找不到符合关键词的图片";
                    break;
                case 429:
                    error_msg = "达到调用额度限制";
            }
            e.getGroup().sendMessage("[ACGPro] " + error_msg);
            return;
        }
        Json json = gson.fromJson(content, Json.class);
        /*
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                stop();
                Main.ispulling = false;
                e.getGroup().sendMessage("[ACGPro] 请求超时");
                this.cancel();
            }
        };
        new Timer().schedule(timerTask, 80 * 1000);
        */
        if (picnum == 1) {
            Random random = new Random();
            short index = (short) random.nextInt(json.data.size());
            Data data = json.data.get(index);
            e.getGroup().sendMessage("[ACGPro] 正在从服务器下载图片...");
            Image image = null;
            Main.ispulling = true;
            try {
                image = e.getGroup().uploadImage(new URL(data.url.replace("i.pixiv.cat", "pixivi.sakuralo.top")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (image == null) {
                e.getGroup().sendMessage("[ACGPro] 图片下载错误");
                Main.ispulling = false;
                return;
            }
            e.getGroup().sendMessage(MessageUtils.newChain(image)
                    .plus("作品标题: " + data.title + "\nPid: " + data.pid + "\n作者名: " + data.author + "\n作者UID: " + data.uid));
        } else {
            if (picnum > json.data.size()) picnum = (short) json.data.size();
            e.getGroup().sendMessage("[ACGPro] 正在从服务器下载" + picnum + "张图片...");
            for (short a = 0; a < picnum; a++) {
                Data data = json.data.get(a);
                Image image = null;
                Main.ispulling = true;
                try {
                    image = e.getGroup().uploadImage(new URL(data.url.replace("i.pixiv.cat", "pixivi.sakuralo.top")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (image == null) {
                    e.getGroup().sendMessage("[ACGPro] 图片下载错误");
                    Main.ispulling = false;
                    continue;
                }
                e.getGroup().sendMessage(MessageUtils.newChain(image)
                        .plus("作品标题: " + data.title + "\nPid: "
                                + data.pid + "\n作者名: " + data.author
                                + "\n作者UID: " + data.uid
                                + "\n[" + (a + 1) + "/" + picnum + "]"));
            }
        }
        //timerTask.cancel();
        Main.ispulling = false;
    }
}