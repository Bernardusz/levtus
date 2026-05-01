import io.github.bernardusz.levtus.Levtus;

public class Main {
    public static void main(String[] args) {
        Levtus app = Levtus.create();

        // A simple GET route
        app.get("/hello", ctx -> {
            ctx.res().send("Hello from the Levtus Engine!");
        });

        // A POST route for data processing
        app.post("/data", ctx -> {
            byte[] body = ctx.req().body();
            // Action: Save to DB or Cloud
            ctx.res().status(201).send("Data Received");
        });

        app.listen(8080);
    }
}