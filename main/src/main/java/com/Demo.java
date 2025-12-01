package com;

public class Demo {

    public static void main(String[] args) throws Exception {
        final int n = 6;
        final double delayMin = 0.25;
        final double modelMin = 50;
        var model = LiftObject.CreateLiftModel(n, delayMin);
        model.setProtocolPrint(true);
        model.go(modelMin);
        System.out.println("Average waiting time in queues:");
        for (int i = 0; i < 5; i++) {
            var floor = model.findObj("Поверх %d".formatted(i + 1));
            var queue = floor.findPlace("Очік на п%d".formatted(i + 1));
            var meanLength = queue.getMean();
            var wereInTotal = floor.findPlace("Всього було на п%d".formatted(i + 1)).getMark();
            System.out.println("Floor %d: %f m".formatted(i + 1, meanLength * model.getCurrentTime() / wereInTotal));
        }

        var waitsTimeRatio = model.findObj("Логіка очікування").findPlace("Чекає").getMean();
        var movesEmptyTimeRatio = model.findObj("Рух ліфту").findPlace("Рухається порожній").getMean();
        var movesWithPassengersTimeRatio = 1 - waitsTimeRatio - movesEmptyTimeRatio;
        System.out.println("Time ratio lift waits: %.3f".formatted(waitsTimeRatio));
        System.out.println("Time ratio lift moves empty: %.3f".formatted(movesEmptyTimeRatio));
        System.out.println("Time ratio lift moves with passengers: %.3f".formatted(movesWithPassengersTimeRatio));

        var pFreePlaces = model.findObj("Рух ліфту").findPlace("Вільних місць");
        var maxAmountInLift = n - pFreePlaces.getObservedMin();
        System.out.println("Max amount of people in lift: %d".formatted(maxAmountInLift));
        var meanAmountInLift = n - pFreePlaces.getMean();
        System.out.println("Mean amount of people in lift: %.3f".formatted(meanAmountInLift));

        System.out.println("Ratio of people that failed to take place in lift:");
        for (int ifloor = 0; ifloor < 5; ifloor++) {
            var floor = model.findObj("Поверх %d".formatted(ifloor + 1));
            var satCnt = floor.findPlace("Сіли в ліфт на п%d".formatted(ifloor + 1)).getMark();
            var totalCnt = floor.findPlace("Всього було на п%d".formatted(ifloor + 1)).getMark();
            System.out.println("On floor %d: %.3f".formatted(ifloor + 1, 1 - satCnt / (float) totalCnt));
        }
    }
}
