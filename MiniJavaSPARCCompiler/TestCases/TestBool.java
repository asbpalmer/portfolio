class TestBool {
    public static void main(String[] a){
        if ( 2 < 1 ){
            System.out.println(0);
        } else {
            System.out.println(new BoolTest().eval(20));
        }
    }
}

class BoolTest {
    public int eval(int t){
        int a;

        if (!(t < 10 && true)) {
            a = t * t;
        } else {
            a = t + t;
        }

        return a;
    }
}