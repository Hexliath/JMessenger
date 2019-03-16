package model;

public enum Rights {


        ADMIN ("Edit, delete and read channel"),
        OWNER ("Own the channel"),
        MEMBER ("Read and publish only");

        private String typename = "";
        Rights(String name){
            this.typename = name;
        }
        public String toString(){
            return typename;
        }
    }

