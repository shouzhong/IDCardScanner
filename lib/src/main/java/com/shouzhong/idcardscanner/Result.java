package com.shouzhong.idcardscanner;

public class Result {

    public int type;// 1为正面，2为反面
    public String cardNum;// 身份证号
    public String name;// 名字
    public String sex;// 性别
    public String address;// 地址
    public String nation;// 民族
    public String birth;// 出生年月日：yyyy-MM-dd
    public String office;// 签发机关
    public String validDate;// 有限期限：yyyyMMdd-yyyyMMdd

    public String path;// 照片

    @Override
    public String toString() {
        String text = "";
        if (type == 1) {
            text += "\nname:" + name;
            text += "\nnumber:" + cardNum;
            text += "\nsex:" + sex;
            text += "\nnation:" + nation;
            text += "\nbirth:" + birth;
            text += "\naddress:" + address;
            text += "\npath:" + path;
        } else if (type == 2) {
            text += "\noffice:" + office;
            text += "\nvalidDate:" + validDate;
            text += "\npath:" + path;
        }
        return text;
    }
}
