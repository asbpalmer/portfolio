class TestOverload {
    public static void main(String[] a){
        System.out.println(new T().overload(3));
    }
}


class T {
    public int overload(){
        return this.overload(0);
    }

    public int overload(int a){
        return (a + a) * a;
    }
}