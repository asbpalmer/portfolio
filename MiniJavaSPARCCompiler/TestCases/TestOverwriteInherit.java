class TestOverwriteInherit {
    public static void main(String[] a){
        System.out.println(new T1().overload(3));
    }
}

class TB {
    public int overload(int a){
        return a;
    }
}

class T1 extends TB {

    public int overload(int a){
        return a + 4 * a;
    }
}