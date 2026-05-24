package application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

public class TimeCounter {
    private static Timer globalTimer = new Timer(true);

    // Статический метод для расчета статуса (без TimerTask)
    public static CareStatus computeStatus(LocalDate lastDate, int intervalDays, LocalDate today) {
        if (lastDate == null) return CareStatus.RED;
        long daysSince = ChronoUnit.DAYS.between(lastDate, today);
        long daysToNext = intervalDays - daysSince;

        if (daysToNext <= 0) return CareStatus.RED;
        if (daysToNext <= 2) return CareStatus.YELLOW;
        return CareStatus.GREEN;
    }

    // Запланировать задачу (для напоминаний)
    public static void scheduleReminder(Runnable task, long delaySeconds) {
        globalTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, delaySeconds * 1000);
    }
}