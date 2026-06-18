// sources/errors.ts

export type SourceErrorKind =
  | 'network'
  | 'timeout'
  | 'http'
  | 'parse'
  | 'partial';

export abstract class SourceError extends Error {
  abstract readonly kind: SourceErrorKind;

  constructor(
    public readonly source: string,
    message?: string,
    public readonly cause?: unknown,
  ) {
    super(message);
    this.name = new.target.name;
  }
}

export class NetworkSourceError extends SourceError {
  readonly kind = 'network' as const;

  constructor(source: string, cause?: unknown) {
    super(source, `${source}: network request failed`, cause);
    this.name = 'NetworkSourceError';
  }
}

export class TimeoutSourceError extends SourceError {
  readonly kind = 'timeout' as const;

  constructor(source: string) {
    super(source, `${source}: request timed out`);
    this.name = 'TimeoutSourceError';
  }
}

export class HttpSourceError extends SourceError {
  readonly kind = 'http' as const;

  constructor(source: string, public readonly status: number) {
    super(source, `${source}: HTTP ${status}`);
    this.name = 'HttpSourceError';
  }
}

export class ParseSourceError extends SourceError {
  readonly kind = 'parse' as const;

  constructor(source: string, cause?: unknown) {
    super(source, `${source}: failed to parse response`, cause);
    this.name = 'ParseSourceError';
  }
}

export class PartialSourceError extends SourceError {
  readonly kind = 'partial' as const;

  constructor(public readonly sourceErrors: Record<string, SourceError>) {
    const failedSources = Object.keys(sourceErrors).join(', ');
    super('combined', `Some sources failed: ${failedSources}`);
    this.name = 'PartialSourceError';
  }
}
