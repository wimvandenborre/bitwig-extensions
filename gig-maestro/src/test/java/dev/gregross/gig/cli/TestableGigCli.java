package dev.gregross.gig.cli;

/**
 * Test subclass of GigCli that returns a FakeRpcClient instead of a real one.
 */
class TestableGigCli extends GigCli {

    private final FakeRpcClient fakeClient = new FakeRpcClient();

    @Override
    RpcClient createClient() {
        return fakeClient;
    }

    FakeRpcClient getFakeClient() {
        return fakeClient;
    }
}
