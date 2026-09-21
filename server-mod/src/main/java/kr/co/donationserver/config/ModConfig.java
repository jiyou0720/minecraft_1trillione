package kr.co.donationserver.config;

import net.minecraftforge.common.config.Configuration;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public final class ModConfig {
    public static long goal;
    public static int teleportDelaySeconds, teleportCooldownSeconds;
    public static boolean tutorialEnabled;
    public static String[] tutorialMessages;
    public static long[] milestones;
    public static String[] milestoneTitles;

    private ModConfig() {}

    public static void load(File file) {
        Configuration c = new Configuration(file, "1");
        c.load();
        goal = parsePositive(c.get("donation", "serverDonationGoal", "100000000", "서버 전체 기부 목표").getString(), 100000000L);
        milestones = parseLongs(c.getStringList("personalMilestones", "donation", new String[]{"1000000", "10000000", "50000000"}, "개인 누적 기부 보상 구간"));
        milestoneTitles = c.getStringList("milestoneTitles", "donation", new String[]{"기부천사", "큰손", "전설의 후원자"}, "각 구간의 칭호");
        teleportDelaySeconds = c.getInt("delaySeconds", "travel", 3, 0, 30, "이동 대기 시간");
        teleportCooldownSeconds = c.getInt("cooldownSeconds", "travel", 10, 0, 3600, "이동 쿨다운");
        tutorialEnabled = c.getBoolean("enabled", "tutorial", true, "첫 접속 채팅 튜토리얼 사용");
        String[] defaultTutorial = new String[]{
                "&e어서 오세요! &f해당 서버는 경제를 중심으로 돌아가는 서버입니다.",
                "&f농사, 광질, 낚시, 요리 등의 콘텐츠로 돈을 벌 수 있습니다. 번 돈으로 인챈트, 스킨, 펫, 탈것 등을 구매할 수 있습니다.",
                "&fShift+F로 거점 이동 메뉴를 열어 이동할 수 있습니다.",
                "&f첫 영지 1청크는 무료입니다. 청크를 더 넓히려면 땅을 구매해야 합니다."};
        tutorialMessages = c.getStringList("messages", "tutorial", defaultTutorial, "채팅 튜토리얼 페이지. 각 항목이 한 페이지입니다.");
        String[] legacyTutorial = {
                "&6[튜토리얼] &f어서 오세요! 이동과 상호작용 방법을 익혀주세요.",
                "&6[튜토리얼] &f농작물과 음식을 판매해 서버 화폐를 벌 수 있습니다.",
                "&6[튜토리얼] &fShift+F로 거점 이동 메뉴를 열 수 있습니다.",
                "&6[튜토리얼] &f모두 힘을 합쳐 1억 원 기부 목표를 달성하세요!"};
        if (Arrays.equals(tutorialMessages, legacyTutorial)) {
            tutorialMessages = defaultTutorial;
            c.get("tutorial", "messages", defaultTutorial).set(defaultTutorial);
        }
        if (c.hasChanged()) c.save();
    }

    private static long[] parseLongs(String[] values) {
        List<Long> out = new ArrayList<>();
        for (String value : values) try { long n = Long.parseLong(value); if (n > 0) out.add(n); } catch (NumberFormatException ignored) {}
        Collections.sort(out);
        long[] result = new long[out.size()];
        for (int i = 0; i < result.length; i++) result[i] = out.get(i);
        return result;
    }
    private static long parsePositive(String value,long fallback){try{long n=new BigDecimal(value.trim()).longValueExact();return n>0?n:fallback;}catch(Exception ignored){return fallback;}}
}
