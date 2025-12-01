package com;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ua.stetsenkoinna.PetriObj.*;

public final class LiftObject {

    public static PetriObjModel CreateLiftModel(int n, double liftDel) throws Exception {
        final int floors = 5;
        HashMap<String, PetriP> shrPlaces = new HashMap<>();
        shrPlaces.put(liftFloor, new PetriP(liftFloor, 1));
        shrPlaces.put(freePlaces, new PetriP(freePlaces, n));
        for (String emptyPlaceName : List.of(checkStop, setDirection, calls, down, up, passengersChecked, setNextFloor, nextFloor, checkOut, checkWaiting, doorsOpen, floorNotSet)) {
            shrPlaces.put(emptyPlaceName, new PetriP(emptyPlaceName, 0));
        }
        ArrayList<PetriP> goTo = new ArrayList<>(floors);
        ArrayList<PetriP> leaveOn = new ArrayList<>(floors);
        ArrayList<PetriP> waitingOn = new ArrayList<>(floors);
        ArrayList<PetriP> liftOnFloor = new ArrayList<>(floors);
        for (int i = 0; i < floors; i++) {
            goTo.add(new PetriP(t_goTo.formatted(i + 1), 0));
            leaveOn.add(new PetriP(t_leaveOn.formatted(i + 1), 0));
            waitingOn.add(new PetriP(t_waitingOn.formatted(i + 1), 0));
            liftOnFloor.add(new PetriP(t_liftOnFloor.formatted(i + 1), 0));
        }
        ArrayList<PetriSim> model = new ArrayList<>();
        model.add(new PetriSim(CreateLiftMovement(n, liftDel, shrPlaces)));
        model.getLast().setPriority(1);
        model.add(new PetriSim(CreateNetWaitingLogic(n, shrPlaces)));
        model.getLast().setPriority(2);
        model.add(new PetriSim(CreateLeaveLift(shrPlaces, n, goTo, leaveOn, liftOnFloor)));
        model.getLast().setPriority(3);
        model.add(new PetriSim(CreateNetFloorToGo(shrPlaces, goTo, waitingOn)));
        model.getLast().setPriority(2);
        model.add(new PetriSim(CreateFirstFloor(waitingOn.get(0), liftOnFloor.get(0), goTo, shrPlaces)));
        model.getLast().setPriority(4);
        for (int i = 1; i < 4; i++) {
            model.add(new PetriSim(CreateFloorI(i, leaveOn.get(i), waitingOn.get(i), liftOnFloor.get(i), goTo, shrPlaces)));
            model.getLast().setPriority(4);
        }
        model.add(new PetriSim(CreateLastFloor(leaveOn.getLast(), waitingOn.getLast(), liftOnFloor.getLast(), goTo, shrPlaces)));
        model.getLast().setPriority(4);
        return new PetriObjModel(model);
    }

    private static PetriNet CreateFirstFloor(PetriP waitingOnFirst, PetriP liftOnFloorFirst, List<PetriP> goTo, Map<String, PetriP> shrPlaces) throws Exception {
        ArrayList<PetriP> d_P = new ArrayList<>();
        ArrayList<PetriT> d_T = new ArrayList<>();
        ArrayList<ArcIn> d_In = new ArrayList<>();
        ArrayList<ArcOut> d_Out = new ArrayList<>();
        PetriP.initNext();
        PetriT.initNext();
        ArcIn.initNext();
        ArcOut.initNext();

        d_P.add(new PetriP("Приходять до 1", 1));
        d_T.add(new PetriT("Нова людина", 1, 2));
        d_T.getLast().setDistribution("exp", 1);
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        PetriP setNextFloor = new PetriP("Визначити наст пов");
        d_P.add(setNextFloor);
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(new PetriP("Збільшити кількість"));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        d_T.add(new PetriT("", 0, 2));
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        d_P.add(new PetriP("Всього було на п1"));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(shrPlaces.get(calls).refreshNumber());
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(waitingOnFirst.refreshNumber());
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        PetriP entrance = new PetriP("Вхід");
        d_P.add(entrance);
        d_P.add(shrPlaces.get(checkWaiting).refreshNumber());
        d_P.add(liftOnFloorFirst.refreshNumber());

        d_T.add(new PetriT("Ліфт зупиняється", 0, 0));
        d_In.add(new ArcIn(shrPlaces.get(checkWaiting), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorFirst, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), entrance, 1));

        d_P.add(shrPlaces.get(doorsOpen).refreshNumber());
        d_T.add(new PetriT("", 0, 0));
        d_In.add(new ArcIn(shrPlaces.get(doorsOpen), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorFirst, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), entrance, 1));

        d_T.add(new PetriT("Двері зачиняються", 0, 0));
        d_In.add(new ArcIn(entrance, d_T.getLast()));
        d_P.add(shrPlaces.get(passengersChecked).refreshNumber());
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        PetriP satInLift = new PetriP("Сіли в ліфт на п1");
        d_P.add(satInLift);
        d_P.add(shrPlaces.get(freePlaces).refreshNumber());

        for (int i = 1; i < 5; i++) {
            d_T.add(new PetriT("На %d".formatted(i + 1), 0, 2));
            d_T.getLast().setProbability(0.25);
            d_In.add(new ArcIn(setNextFloor, d_T.getLast()));
            d_P.add(new PetriP("До %d".formatted(i + 1)));
            d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

            d_T.add(new PetriT("", 0, 1));
            d_In.add(new ArcIn(shrPlaces.get(calls), d_T.getLast()));
            d_In.add(new ArcIn(waitingOnFirst, d_T.getLast()));
            d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
            d_In.add(new ArcIn(entrance, d_T.getLast(), 1, true));
            d_In.add(new ArcIn(shrPlaces.get(freePlaces), d_T.getLast()));
            d_Out.add(new ArcOut(d_T.getLast(), satInLift, 1));
            d_P.add(goTo.get(i).refreshNumber());
            d_Out.add(new ArcOut(d_T.getLast(), goTo.get(i), 1));
        }

        PetriNet res = new PetriNet("Поверх 1", d_P, d_T, d_In, d_Out);

        return res;
    }

    private static PetriNet CreateLastFloor(PetriP leftOnLast, PetriP waitingOnLast, PetriP liftOnFloorLast, List<PetriP> goTo, Map<String, PetriP> shrPlaces) throws Exception {
        final int floor = 5;
        ArrayList<PetriP> d_P = new ArrayList<>();
        ArrayList<PetriT> d_T = new ArrayList<>();
        ArrayList<ArcIn> d_In = new ArrayList<>();
        ArrayList<ArcOut> d_Out = new ArrayList<>();
        PetriP.initNext();
        PetriT.initNext();
        ArcIn.initNext();
        ArcOut.initNext();

        d_P.add(leftOnLast.refreshNumber());
        d_T.add(new PetriT("Перебувають на поверсі", 0, 2));
        d_T.getLast().setDistribution("unif", 67.5);
        d_T.getLast().setParamDeviation(52.5);
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        PetriP setNextFloor = new PetriP("Визначити наст пов");
        d_P.add(setNextFloor);
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(new PetriP("Збільшити кількість"));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        d_T.add(new PetriT("", 0, 2));
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        d_P.add(new PetriP("Всього було на п%d".formatted(floor)));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(shrPlaces.get(calls).refreshNumber());
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(waitingOnLast.refreshNumber());
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        PetriP entrance = new PetriP("Вхід");
        d_P.add(entrance);
        d_P.add(shrPlaces.get(checkWaiting).refreshNumber());
        d_P.add(liftOnFloorLast.refreshNumber());

        d_T.add(new PetriT("Ліфт зупиняється", 0, 0));
        d_In.add(new ArcIn(shrPlaces.get(checkWaiting), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorLast, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), entrance, 1));

        d_P.add(shrPlaces.get(doorsOpen).refreshNumber());
        d_T.add(new PetriT("", 0, 0));
        d_In.add(new ArcIn(shrPlaces.get(doorsOpen), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorLast, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), entrance, 1));

        d_P.add(shrPlaces.get(passengersChecked).refreshNumber());
        d_T.add(new PetriT("Двері зачиняються", 0, 0));
        d_In.add(new ArcIn(entrance, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        PetriP satInLift = new PetriP("Сіли в ліфт на п%d".formatted(floor));
        d_P.add(satInLift);
        d_P.add(shrPlaces.get(freePlaces).refreshNumber());

        for (int i = 0; i < 4; i++) {
            d_T.add(new PetriT("На %d".formatted(i + 1), 0, 2));
            d_T.getLast().setProbability(i == 0 ? 0.7 : 0.1);
            d_In.add(new ArcIn(setNextFloor, d_T.getLast()));
            d_P.add(new PetriP("До %d".formatted(i + 1)));
            d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

            d_T.add(new PetriT("", 0, 1));
            d_In.add(new ArcIn(shrPlaces.get(calls), d_T.getLast()));
            d_In.add(new ArcIn(waitingOnLast, d_T.getLast()));
            d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
            d_In.add(new ArcIn(entrance, d_T.getLast(), 1, true));
            d_In.add(new ArcIn(shrPlaces.get(freePlaces), d_T.getLast()));
            d_Out.add(new ArcOut(d_T.getLast(), satInLift, 1));
            d_P.add(goTo.get(i).refreshNumber());
            d_Out.add(new ArcOut(d_T.getLast(), goTo.get(i), 1));
        }

        PetriNet res = new PetriNet("Поверх %d".formatted(floor), d_P, d_T, d_In, d_Out);

        return res;
    }

    // zero based index must be used (1 means second floor, 2 - third, etc.)
    private static PetriNet CreateFloorI(int i, PetriP leftOnI, PetriP waitingOnI, PetriP liftOnFloorI, List<PetriP> goTo, Map<String, PetriP> shrPlaces) throws Exception {
        if (i < 1 || i > 3) {
            throw new Exception("Invalid number of floor. This method can be used only for floors that are not first nor last");
        }
        ArrayList<PetriP> d_P = new ArrayList<>();
        ArrayList<PetriT> d_T = new ArrayList<>();
        ArrayList<ArcIn> d_In = new ArrayList<>();
        ArrayList<ArcOut> d_Out = new ArrayList<>();
        PetriP.initNext();
        PetriT.initNext();
        ArcIn.initNext();
        ArcOut.initNext();

        d_P.add(leftOnI.refreshNumber());
        d_T.add(new PetriT("Перебувають на поверсі", 0, 2));
        d_T.getLast().setDistribution("unif", 67.5);
        d_T.getLast().setParamDeviation(52.5);
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        PetriP setNextFloor = new PetriP("Визначити наст пов");
        d_P.add(setNextFloor);
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(new PetriP("Збільшити кількість"));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        d_T.add(new PetriT("", 0, 2));
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        d_P.add(new PetriP("Всього було на п%d".formatted(i + 1)));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(shrPlaces.get(calls).refreshNumber());
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
        d_P.add(waitingOnI.refreshNumber());
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        PetriP waitingDown = new PetriP("Чекають вниз");
        d_P.add(waitingDown);
        PetriP waitingUp = new PetriP("Чекають вгору");
        d_P.add(waitingUp);
        PetriP entrance = new PetriP("Вхід");
        d_P.add(entrance);
        d_P.add(shrPlaces.get(checkWaiting).refreshNumber());
        d_P.add(liftOnFloorI.refreshNumber());

        d_T.add(new PetriT("Ліфт зупиняється", 0, 1));
        d_In.add(new ArcIn(shrPlaces.get(checkWaiting), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorI, d_T.getLast()));
        d_In.add(new ArcIn(waitingDown, d_T.getLast(), 1, true));
        d_P.add(shrPlaces.get(down).refreshNumber());
        d_In.add(new ArcIn(shrPlaces.get(down), d_T.getLast(), 1, true));
        d_Out.add(new ArcOut(d_T.getLast(), entrance, 1));

        d_T.add(new PetriT("Ліфт зупиняється", 0, 1));
        d_In.add(new ArcIn(shrPlaces.get(checkWaiting), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorI, d_T.getLast()));
        d_In.add(new ArcIn(waitingUp, d_T.getLast(), 1, true));
        d_P.add(shrPlaces.get(up).refreshNumber());
        d_In.add(new ArcIn(shrPlaces.get(up), d_T.getLast(), 1, true));
        d_Out.add(new ArcOut(d_T.getLast(), entrance, 1));

        d_T.add(new PetriT("Ліфт зупиняється", 0, 1));
        d_In.add(new ArcIn(shrPlaces.get(checkWaiting), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorI, d_T.getLast()));
        d_P.add(shrPlaces.get(floorNotSet).refreshNumber());
        d_In.add(new ArcIn(shrPlaces.get(floorNotSet), d_T.getLast(), 1, true));
        d_Out.add(new ArcOut(d_T.getLast(), entrance, 1));

        d_T.add(new PetriT("Двері не відчиняються", 0, 0));
        d_In.add(new ArcIn(shrPlaces.get(checkWaiting), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorI, d_T.getLast()));
        d_P.add(shrPlaces.get(passengersChecked).refreshNumber());
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        d_T.add(new PetriT("", 0, 0));
        d_P.add(shrPlaces.get(doorsOpen).refreshNumber());
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloorI, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), entrance, 1));

        d_T.add(new PetriT("Двері зачиняються", 0, 0));
        d_In.add(new ArcIn(entrance, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), shrPlaces.get(passengersChecked), 1));

        PetriP satInLift = new PetriP("Сіли в ліфт на п%d".formatted(i + 1));
        d_P.add(satInLift);
        d_P.add(shrPlaces.get(freePlaces).refreshNumber());

        for (int j = 0; j < 5; j++) {
            if (j == i) {
                continue;
            }

            d_T.add(new PetriT("На %d".formatted(j + 1), 0, 2));
            d_T.getLast().setProbability(j == 0 ? 0.7 : 0.1);
            d_In.add(new ArcIn(setNextFloor, d_T.getLast()));
            d_P.add(new PetriP("До %d".formatted(j + 1)));
            d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));
            d_Out.add(new ArcOut(d_T.getLast(), j < i ? waitingDown : waitingUp, 1));

            d_T.add(new PetriT("", 0, 1));
            d_In.add(new ArcIn(shrPlaces.get(calls), d_T.getLast()));
            d_In.add(new ArcIn(waitingOnI, d_T.getLast()));
            d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
            d_In.add(new ArcIn(entrance, d_T.getLast(), 1, true));
            d_In.add(new ArcIn(shrPlaces.get(freePlaces), d_T.getLast()));
            d_In.add(new ArcIn(j < i ? waitingDown : waitingUp, d_T.getLast()));
            d_Out.add(new ArcOut(d_T.getLast(), satInLift, 1));
            d_P.add(goTo.get(j).refreshNumber());
            d_Out.add(new ArcOut(d_T.getLast(), goTo.get(j), 1));
        }

        PetriNet res = new PetriNet("Поверх %d".formatted(i + 1), d_P, d_T, d_In, d_Out);

        return res;
    }

    private static PetriNet CreateLeaveLift(Map<String, PetriP> shrPlaces, int n, List<PetriP> goTo, List<PetriP> leaveOn, List<PetriP> liftOnFloor) throws Exception {
        ArrayList<PetriP> d_P = new ArrayList<>();
        ArrayList<PetriT> d_T = new ArrayList<>();
        ArrayList<ArcIn> d_In = new ArrayList<>();
        ArrayList<ArcOut> d_Out = new ArrayList<>();
        PetriP.initNext();
        PetriT.initNext();
        ArcIn.initNext();
        ArcOut.initNext();

        PetriP setFloor = new PetriP("Визначити поверх", 0);
        d_P.add(setFloor);
        d_T.add(new PetriT("", 0));
        d_P.add(shrPlaces.get(checkOut).refreshNumber());
        d_In.add(new ArcIn(shrPlaces.get(checkOut), d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), setFloor, 1));
        d_P.add(new PetriP("", 0));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        d_T.add(new PetriT("", 0));
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        d_P.add(new PetriP("Визначити чи був вихід", 0));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

        PetriP counter = new PetriP("", n);
        d_T.add(new PetriT("Ніхто не вийшов", 0, 2));
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        d_In.add(new ArcIn(counter, d_T.getLast(), n, true));
        d_Out.add(new ArcOut(d_T.getLast(), shrPlaces.get(checkWaiting).refreshNumber(), 1));
        d_T.add(new PetriT("Хтось вийшов", 0));
        d_In.add(new ArcIn(d_P.getLast(), d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), shrPlaces.get(doorsOpen).refreshNumber(), 1));
        d_P.add(counter);
        d_P.add(shrPlaces.get(checkWaiting));
        d_P.add(shrPlaces.get(doorsOpen));
        d_P.add(shrPlaces.get(liftFloor).refreshNumber());
        d_P.add(shrPlaces.get(freePlaces).refreshNumber());

        for (int i = 0; i < 5; i++) {
            d_T.add(new PetriT("", 0, i));
            d_P.add(liftOnFloor.get(i).refreshNumber());
            d_In.add(new ArcIn(shrPlaces.get(liftFloor), d_T.getLast(), i + 1, true));
            d_In.add(new ArcIn(setFloor, d_T.getLast()));
            d_Out.add(new ArcOut(d_T.getLast(), d_P.getLast(), 1));

            d_T.add(new PetriT("", 0, 3));
            d_In.add(new ArcIn(d_P.getLast(), d_T.getLast(), 1, true));
            d_P.add(goTo.get(i).refreshNumber());
            d_In.add(new ArcIn(goTo.get(i), d_T.getLast()));
            d_In.add(new ArcIn(counter, d_T.getLast()));
            d_Out.add(new ArcOut(d_T.getLast(), counter, 1));
            d_Out.add(new ArcOut(d_T.getLast(), shrPlaces.get(freePlaces), 1));
            d_P.add(leaveOn.get(i).refreshNumber());
            d_Out.add(new ArcOut(d_T.getLast(), leaveOn.get(i), 1));
        }

        PetriNet res = new PetriNet("Вихід з ліфту", d_P, d_T, d_In, d_Out);

        return res;
    }

    private static PetriNet CreateNetFloorToGo(Map<String, PetriP> shrPlaces, List<PetriP> goTo, List<PetriP> waitingOn) throws Exception {
        ArrayList<PetriP> d_P = new ArrayList<>();
        ArrayList<PetriT> d_T = new ArrayList<>();
        ArrayList<ArcIn> d_In = new ArrayList<>();
        ArrayList<ArcOut> d_Out = new ArrayList<>();
        PetriP.initNext();
        PetriT.initNext();
        ArcIn.initNext();
        ArcOut.initNext();
        d_P.add(shrPlaces.get(setNextFloor).refreshNumber());
        d_P.add(shrPlaces.get(nextFloor).refreshNumber());
        d_T.add(new PetriT("", 0));
        d_In.add(new ArcIn(d_P.get(0), d_T.getLast(), 1));
        d_Out.add(new ArcOut(d_T.getLast(), d_P.get(1), 1));
        for (int i = 0; i < goTo.size(); i++) {
            PetriP p = goTo.get(i).refreshNumber();
            PetriT t = new PetriT("На %d".formatted(i + 1), 0);
            t.setPriority(2);
            d_In.add(new ArcIn(p, t));
            d_In.getLast().setInf(true);
            d_In.add(new ArcIn(d_P.get(0), t));
            d_Out.add(new ArcOut(t, d_P.get(1), i + 1));

            d_P.add(p);
            d_T.add(t);
        }
        for (int i = 1; i < waitingOn.size(); i++) {
            PetriP p = waitingOn.get(i).refreshNumber();
            PetriT t = new PetriT("На %d".formatted(i + 1), 0);
            t.setPriority(1);
            d_In.add(new ArcIn(p, t));
            d_In.getLast().setInf(true);
            d_In.add(new ArcIn(d_P.get(0), t));
            d_Out.add(new ArcOut(t, d_P.get(1), i + 1));

            d_P.add(p);
            d_T.add(t);
        }
        PetriNet d_Net = new PetriNet("Визначення поверху", d_P, d_T, d_In, d_Out);

        return d_Net;
    }

    private static PetriNet CreateLiftMovement(int n, double liftDel, Map<String, PetriP> shrPlaces) throws Exception {

        ArrayList<PetriP> d_P = new ArrayList<>();
        ArrayList<PetriT> d_T = new ArrayList<>();
        ArrayList<ArcIn> d_In = new ArrayList<>();
        ArrayList<ArcOut> d_Out = new ArrayList<>();
        PetriP.initNext();
        PetriT.initNext();
        ArcIn.initNext();
        ArcOut.initNext();
        d_P.add(shrPlaces.get(freePlaces).refreshNumber());
        d_P.add(shrPlaces.get(checkOut).refreshNumber());
        d_P.add(shrPlaces.get(up).refreshNumber());
        d_P.add(new PetriP("P3", 0)); // lag
        d_P.add(shrPlaces.get(setDirection).refreshNumber());
        d_P.add(shrPlaces.get(floorNotSet).refreshNumber());
        d_P.add(new PetriP("Очистити 'До'", 0));
        d_P.add(new PetriP("Рухається порожній", 0));
        d_P.add(new PetriP("", 1));
        d_P.add(new PetriP("Почати рух", 0));
        d_P.add(new PetriP("Прямує до", 1));
        d_P.add(shrPlaces.get(down).refreshNumber());
        d_P.add(shrPlaces.get(setNextFloor).refreshNumber());
        d_P.add(new PetriP("", 1));
        d_P.add(shrPlaces.get(nextFloor).refreshNumber());
        d_P.add(shrPlaces.get(checkStop).refreshNumber());
        d_P.add(shrPlaces.get(passengersChecked).refreshNumber());
        d_P.add(shrPlaces.get(liftFloor).refreshNumber());
        d_P.add(new PetriP("Стоїть", 1));

        d_T.add(new PetriT("", 0.0)); // 0
        d_T.get(0).setPriority(1);
        d_T.add(new PetriT("Доїхав", 0.0)); // 1
        d_T.get(1).setPriority(1);
        d_T.add(new PetriT("До > поверх", 0.0)); // 2
        d_T.get(2).setPriority(2);
        d_T.add(new PetriT("Почав рухатися порожнім", 0.0)); // 3
        d_T.get(3).setPriority(2);
        d_T.add(new PetriT("Поверх > до", 0.0)); // 4
        d_T.get(4).setPriority(2);
        d_T.add(new PetriT("", 0.0)); // 5
        d_T.get(5).setPriority(4);
        d_T.add(new PetriT("", 0.0)); // 6
        d_T.add(new PetriT("Спуск", liftDel)); // 7
        d_T.get(7).setPriority(1);
        d_T.add(new PetriT("Зупинився", 0.0)); // 8
        d_T.add(new PetriT("", 0.0)); // 9
        d_T.get(9).setPriority(1);
        d_T.add(new PetriT("", 0.0)); // 10
        d_T.get(10).setPriority(2);
        d_T.add(new PetriT("", 0.0)); // 11
        d_T.get(11).setPriority(3);
        d_T.add(new PetriT("", 0.0)); // 12
        d_T.get(12).setPriority(3);
        d_T.add(new PetriT("Підйом", liftDel)); // 13
        d_T.get(13).setPriority(1);
        d_In.add(new ArcIn(d_P.get(14), d_T.get(0), 1));
        d_In.add(new ArcIn(d_P.get(4), d_T.get(1), 1));
        d_In.add(new ArcIn(d_P.get(4), d_T.get(2), 1));
        d_In.add(new ArcIn(d_P.get(10), d_T.get(2), 1));
        d_In.get(3).setInf(true);
        d_In.add(new ArcIn(d_P.get(9), d_T.get(3), 1));
        d_In.get(4).setInf(true);
        d_In.add(new ArcIn(d_P.get(0), d_T.get(3), n));
        d_In.get(5).setInf(true);
        d_In.add(new ArcIn(d_P.get(8), d_T.get(3), 1));
        d_In.add(new ArcIn(d_P.get(4), d_T.get(4), 1));
        d_In.add(new ArcIn(d_P.get(17), d_T.get(4), 1));
        d_In.get(8).setInf(true);
        d_In.add(new ArcIn(d_P.get(10), d_T.get(5), 1));
        d_In.add(new ArcIn(d_P.get(6), d_T.get(5), 1));
        d_In.get(10).setInf(true);
        d_In.add(new ArcIn(d_P.get(16), d_T.get(6), 1));
        d_In.add(new ArcIn(d_P.get(18), d_T.get(7), 1));
        d_In.add(new ArcIn(d_P.get(9), d_T.get(7), 1));
        d_In.add(new ArcIn(d_P.get(17), d_T.get(7), 1));
        d_In.add(new ArcIn(d_P.get(11), d_T.get(7), 1));
        d_In.add(new ArcIn(d_P.get(7), d_T.get(8), 1));
        d_In.add(new ArcIn(d_P.get(18), d_T.get(8), 1));
        d_In.get(17).setInf(true);
        d_In.add(new ArcIn(d_P.get(5), d_T.get(9), 1));
        d_In.add(new ArcIn(d_P.get(16), d_T.get(9), 1));
        d_In.add(new ArcIn(d_P.get(13), d_T.get(10), 1));
        d_In.add(new ArcIn(d_P.get(14), d_T.get(10), 1));
        d_In.add(new ArcIn(d_P.get(4), d_T.get(11), 1));
        d_In.get(22).setInf(true);
        d_In.add(new ArcIn(d_P.get(17), d_T.get(11), 1));
        d_In.add(new ArcIn(d_P.get(10), d_T.get(11), 1));
        d_In.add(new ArcIn(d_P.get(6), d_T.get(12), 1));
        d_In.add(new ArcIn(d_P.get(18), d_T.get(13), 1));
        d_In.add(new ArcIn(d_P.get(9), d_T.get(13), 1));
        d_In.add(new ArcIn(d_P.get(2), d_T.get(13), 1));
        d_Out.add(new ArcOut(d_T.get(0), d_P.get(10), 1));
        d_Out.add(new ArcOut(d_T.get(1), d_P.get(6), 1));
        d_Out.add(new ArcOut(d_T.get(1), d_P.get(1), 1));
        d_Out.add(new ArcOut(d_T.get(1), d_P.get(5), 1));
        d_Out.add(new ArcOut(d_T.get(2), d_P.get(2), 1));
        d_Out.add(new ArcOut(d_T.get(2), d_P.get(1), 1));
        d_Out.add(new ArcOut(d_T.get(3), d_P.get(7), 1));
        d_Out.add(new ArcOut(d_T.get(4), d_P.get(11), 1));
        d_Out.add(new ArcOut(d_T.get(4), d_P.get(1), 1));
        d_Out.add(new ArcOut(d_T.get(6), d_P.get(9), 1));
        d_Out.add(new ArcOut(d_T.get(7), d_P.get(18), 1));
        d_Out.add(new ArcOut(d_T.get(7), d_P.get(4), 1));
        d_Out.add(new ArcOut(d_T.get(8), d_P.get(8), 1));
        d_Out.add(new ArcOut(d_T.get(9), d_P.get(12), 1));
        d_Out.add(new ArcOut(d_T.get(10), d_P.get(13), 1));
        d_Out.add(new ArcOut(d_T.get(10), d_P.get(10), 1));
        d_Out.add(new ArcOut(d_T.get(10), d_P.get(4), 1));
        d_Out.add(new ArcOut(d_T.get(10), d_P.get(15), 1));
        d_Out.add(new ArcOut(d_T.get(11), d_P.get(17), 1));
        d_Out.add(new ArcOut(d_T.get(11), d_P.get(10), 1));
        d_Out.add(new ArcOut(d_T.get(13), d_P.get(18), 1));
        d_Out.add(new ArcOut(d_T.get(13), d_P.get(17), 1));
        d_Out.add(new ArcOut(d_T.get(13), d_P.get(4), 1));
        PetriNet d_Net = new PetriNet("Рух ліфту", d_P, d_T, d_In, d_Out);

        return d_Net;
    }

    private static PetriNet CreateNetWaitingLogic(int n, Map<String, PetriP> shrPlaces) throws Exception {
        ArrayList<PetriP> d_P = new ArrayList<>();
        ArrayList<PetriT> d_T = new ArrayList<>();
        ArrayList<ArcIn> d_In = new ArrayList<>();
        ArrayList<ArcOut> d_Out = new ArrayList<>();
        PetriP.initNext();
        PetriT.initNext();
        ArcIn.initNext();
        ArcOut.initNext();
        d_P.add(shrPlaces.get(checkStop).refreshNumber());
        d_P.add(shrPlaces.get(setDirection).refreshNumber());
        var findOutLiftOn1 = new PetriP("Визначити чи ліфт на 1 поверсі");
        d_P.add(findOutLiftOn1);
        var checkDelay = new PetriP("Затримка перевірки");
        d_P.add(checkDelay);
        var findOutCalls = new PetriP("Визначити чи є виклики");
        d_P.add(findOutCalls);
        d_T.add(new PetriT("Початок перевірки", 0));
        d_In.add(new ArcIn(shrPlaces.get(checkStop), d_T.getLast()));
        d_In.add(new ArcIn(shrPlaces.get(setDirection), d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), findOutLiftOn1, 1));
        d_Out.add(new ArcOut(d_T.getLast(), findOutCalls, 1));
        d_Out.add(new ArcOut(d_T.getLast(), checkDelay, 1));

        d_P.add(shrPlaces.get(liftFloor).refreshNumber());
        d_T.add(new PetriT("На 2+ поверсі", 0, 1));
        d_In.add(new ArcIn(findOutLiftOn1, d_T.getLast()));
        d_In.add(new ArcIn(shrPlaces.get(liftFloor), d_T.getLast(), 2, true));

        var liftOnFloor1 = new PetriP("Ліфт на 1 поверсі");
        d_P.add(liftOnFloor1);
        d_T.add(new PetriT("", 0, 0));
        d_In.add(new ArcIn(findOutLiftOn1, d_T.getLast()));
        d_In.add(new ArcIn(shrPlaces.get(liftFloor), d_T.getLast(), 1, true));
        d_Out.add(new ArcOut(d_T.getLast(), liftOnFloor1, 1));

        d_P.add(shrPlaces.get(calls).refreshNumber());
        d_T.add(new PetriT("Викликів > 1", 0, 1));
        d_In.add(new ArcIn(findOutCalls, d_T.getLast()));
        d_In.add(new ArcIn(shrPlaces.get(calls), d_T.getLast(), 1, true));
        var noCalls = new PetriP("Викликів немає");
        d_P.add(noCalls);
        d_T.add(new PetriT("", 0));
        d_In.add(new ArcIn(findOutCalls, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), noCalls, 1));

        d_T.add(new PetriT("", 0));
        d_In.add(new ArcIn(checkDelay, d_T.getLast()));
        var conditionsCheck = new PetriP("Перевірка умов");
        d_P.add(conditionsCheck);
        d_Out.add(new ArcOut(d_T.getLast(), conditionsCheck, 1));

        d_P.add(shrPlaces.get(freePlaces).refreshNumber());
        d_T.add(new PetriT("Початок очікування", 0, 1));
        d_In.add(new ArcIn(shrPlaces.get(freePlaces), d_T.getLast(), n, true));
        d_In.add(new ArcIn(conditionsCheck, d_T.getLast()));
        d_In.add(new ArcIn(liftOnFloor1, d_T.getLast()));
        d_In.add(new ArcIn(noCalls, d_T.getLast()));
        var waits = new PetriP("Чекає", 1);
        d_P.add(waits);
        d_Out.add(new ArcOut(d_T.getLast(), waits, 1));

        d_T.add(new PetriT("Виклик", 0));
        d_In.add(new ArcIn(waits, d_T.getLast()));
        d_In.add(new ArcIn(shrPlaces.get(calls), d_T.getLast(), 1, true));
        d_Out.add(new ArcOut(d_T.getLast(), shrPlaces.get(setDirection), 1));

        d_T.add(new PetriT("Якась умова не виконана", 0));
        d_In.add(new ArcIn(conditionsCheck, d_T.getLast()));
        d_Out.add(new ArcOut(d_T.getLast(), shrPlaces.get(setDirection), 1));

        d_T.add(new PetriT("Не відбулося очікування", 0));
        d_In.add(new ArcIn(liftOnFloor1, d_T.getLast()));

        d_T.add(new PetriT("", 0));
        d_In.add(new ArcIn(noCalls, d_T.getLast()));

        PetriNet d_Net = new PetriNet("Логіка очікування", d_P, d_T, d_In, d_Out);
        return d_Net;
    }

    private static final String liftFloor = "Поверх ліфту";
    private static final String checkStop = "Перевірити умови зупинки";
    private static final String setDirection = "Визначити напрям";
    private static final String freePlaces = "Вільних місць";
    private static final String calls = "Викликів";
    private static final String down = "Вниз";
    private static final String up = "Вгору";
    private static final String passengersChecked = "Пасажирів перевірено";
    private static final String floorNotSet = "Не визн поверх";
    private static final String setNextFloor = "Визн наст поверх";
    private static final String nextFloor = "Наступний поверх";
    private static final String checkOut = "Перевірити вихід";
    private static final String checkWaiting = "Перевірити очікуючих";
    private static final String doorsOpen = "Двері відчинено";

    // Template for numbered
    private static final String t_goTo = "Їдуть до %d";
    private static final String t_leaveOn = "Вийшло на %d";
    private static final String t_waitingOn = "Очік на п%d";
    private static final String t_liftOnFloor = "Ліфт на п%d";
}
