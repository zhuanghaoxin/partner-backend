package cn.ichensw.partner.service;

import cn.ichensw.partner.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
public class InsertUsersTest {

    @Resource
    private UserService userService;

    private ExecutorService executorService = new ThreadPoolExecutor(40, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    public List<String> tagList = Arrays.asList("雅马哈MT-03", "雅马哈YZF-R3", "春风250SR", "豪爵铃木GSX250R", "春风450SR", "凯越321RR", "摩枭500RR", "力帆KPR150", "川崎Ninja400", "奔达金吉拉", "川崎NinjaH2", "宝马G310RR", "五羊本田NX125", "豪爵TR300", "QJMOTOR赛600", "春风250NK", "川崎Z400", "豪爵铃木骊驰GW250", "春风150NK", "春风800NK", "五羊本田帅影150");

    public HashMap<Integer, String> tagMap = new HashMap<Integer, String>();

    @Test
    public void testRandom() {
        List<User> list = userService.list();
        for (int i = 0; i < 30000; i++) {
            int x = new Random().nextInt(1000000);
            User user = list.get(x);
            int j = new Random().nextInt(21);
            int k = new Random().nextInt(21);
            String tag = tagList.get(j);
            String tag2 = tagList.get(k);
            String tagStr;
            if (user.getGender() == 1) {
                tagStr = "[\"男\", \"" + tag + "\", \"" + tag2 + "\"]";
            } else {
                tagStr = "[\"女\", \"" + tag + "\", \"" + tag2 + "\"]";
            }
            user.setTags(tagStr);
            userService.updateById(user);
        }
    }

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers() {
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        final int INSERT_NUM = 100000;
//        List<User> userList = new ArrayList<>();
//        for (int i = 0; i < INSERT_NUM; i++) {
//            User user = new User();
//            user.setUsername("呜呜");
//            user.setUserAccount("fakezhx");
//            user.setAvatarUrl("https://636f-codenav-8grj8px727565176-1256524210.tcb.qcloud.la/img/logo.png");
//            user.setGender(0);
//            user.setUserPassword("12345678");
//            user.setPhone("123");
//            user.setEmail("123@qq.com");
//            user.setTags("[]");
//            user.setUserStatus(0);
//            user.setUserRole(0);
//            userList.add(user);
//        }
//        // 20 秒 10 万条
//        userService.saveBatch(userList, 10000);
//        stopWatch.stop();
//        System.out.println(stopWatch.getTotalTimeMillis());
    }

    /**
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertUsers() {
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        // 分十组
//        int batchSize = 5000;
//        int j = 0;
//        List<CompletableFuture<Void>> futureList = new ArrayList<>();
//        for (int i = 0; i < 100; i++) {
//            List<User> userList = new ArrayList<>();
//            while (true) {
//                j++;
//                User user = new User();
//                String userNameAndAccount = RandomSource.personInfoSource().randomEnglishName();
//                user.setUsername(userNameAndAccount);
//                user.setUserAccount(userNameAndAccount);
//                user.setAvatarUrl("https://636f-codenav-8grj8px727565176-1256524210.tcb.qcloud.la/img/logo.png");
//                int gender = new Random().nextInt(2);
//                user.setGender(gender);
//                user.setUserPassword("12345678");
//                user.setPhone(RandomSource.personInfoSource().randomChineseMobile());
//                user.setEmail(userNameAndAccount + "@qq.com");
//                user.setTags("[]");
//                user.setProfile("");
//                user.setUserStatus(0);
//                user.setUserRole(0);
//                userList.add(user);
//                if (j % batchSize == 0) {
//                    break;
//                }
//            }
//            // 异步执行
//            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                System.out.println("threadName: " + Thread.currentThread().getName());
//                userService.saveBatch(userList, batchSize);
//            }, executorService);
//            futureList.add(future);
//        }
//        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
//        // 20 秒 10 万条
//        stopWatch.stop();
//        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
