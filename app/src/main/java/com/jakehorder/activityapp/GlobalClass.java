package com.jakehorder.activityapp;

import android.app.Application;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.module.AccelerometerBmi160;

public class GlobalClass extends Application {


    // MetaWear Board Objects

    private MetaWearBoard board1;       // left
    private MetaWearBoard board2;       // right

    private AccelerometerBmi160 acc1;
    private AccelerometerBmi160 acc2;

    public MetaWearBoard getBoard1() {
        return board1;
    }

    public void setBoard1(MetaWearBoard board1) {
        this.board1 = board1;
    }

    public MetaWearBoard getBoard2() {
        return board2;
    }

    public void setBoard2(MetaWearBoard board2) {
        this.board2 = board2;
    }

    public AccelerometerBmi160 getAcc1() {
        return acc1;
    }

    public void setAcc1(AccelerometerBmi160 acc1) {
        this.acc1 = acc1;
    }

    public AccelerometerBmi160 getAcc2() {
        return acc2;
    }

    public void setAcc2(AccelerometerBmi160 acc2) {
        this.acc2 = acc2;
    }

    // Time Objects for Sessions and End


}
