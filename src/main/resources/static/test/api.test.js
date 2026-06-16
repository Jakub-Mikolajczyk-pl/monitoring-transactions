import { describe, it, assert, assertEquals, stubFetch, jsonResponse } from './test-runner.js';
import { api, ApiError } from '../js/api.js';

describe('api error handling', () => {
    it('throws ApiError exposing field errors on 400', async () => {
        const restore = stubFetch(() => jsonResponse(400, {
            title: 'Bad request',
            detail: 'invalid',
            errors: [{ field: 'firstName', message: 'must not be blank' }],
        }));
        try {
            await api.customers.create({});
            assert(false, 'expected ApiError to be thrown');
        } catch (error) {
            assert(error instanceof ApiError, 'error is an ApiError');
            assertEquals(error.status, 400);
            assertEquals(error.fieldErrors[0].field, 'firstName');
        } finally {
            restore();
        }
    });

    it('drops empty params and returns the body on success', async () => {
        let capturedUrl = null;
        const restore = stubFetch((url) => {
            capturedUrl = url;
            return jsonResponse(200, { content: [], page: 0, totalPages: 1 });
        });
        const result = await api.alerts.list({ status: 'OPEN', page: 2, size: 20, junk: '' });
        restore();

        assert(capturedUrl.includes('status=OPEN'), 'keeps non-empty status');
        assert(capturedUrl.includes('page=2'), 'keeps page');
        assert(!capturedUrl.includes('junk'), 'drops empty param');
        assertEquals(result.page, 0);
    });
});
