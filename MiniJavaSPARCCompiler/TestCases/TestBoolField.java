class TestBoolField {
    public static void main(String[] a){
        if ( 2 < 1 ){
            System.out.println(0);
        } else {
            System.out.println(new BoolTest().init(100).eval(20));
        }
    }
}

class BoolTest {
    int threshhold;

    public BoolTest init(int t) {
        threshhold = t;
        return this;
    }

    public int eval(int t){
        int a;

        if (t < threshhold) {
            a = threshhold;
        } else {
            a = t;
        }

        return a;
    }
}