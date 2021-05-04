import * as A from "fp-ts/Array"
import * as E from "fp-ts/Either"
import { pipe } from "fp-ts/lib/function"
import * as Separated from "fp-ts/Separated"
import { useMemo, useState } from "react"
import { fromEntries } from "../utils/objects"

export type UseFormStateOptions<T extends object> = {
  initialValues: T
  validators: FormValidators<T>
}

export type FormValidators<T extends object> = {
  [K in keyof T]: Array<FieldValidator<T[K], T>>
}

export type UseFormStateHook<T extends object> = {
  state: FormState<T>
  touch: FormTouchFunction<T>
  set: FormSetFunction<T>
  fieldProps: FormFieldPropsFunction<T>
}

export type FormTouchFunction<T extends object> = (key: keyof T) => void
export type FormSetFunction<T extends object> = (
  key: keyof T,
  value: T[typeof key]
) => void
export type FormFieldPropsFunction<T extends object> = <K extends keyof T>(
  key: K
) => FormFieldProps<T[K]>

export type FormFieldProps<T> = {
  value: T
  onChange: (value: T) => void
  onBlur: () => void
  error?: React.ReactNode
}

export type FormState<T extends object> = {
  [K in keyof T]: FieldState<T[K]>
}

export type FormErrors<T extends object> = Record<keyof T, FieldError[]>

export type FieldState<T> = {
  initialValue: T
  currentValue: T
  touched: boolean
  errors: string[]
}

export type FieldValidator<T, F extends object> = (
  value: T,
  formState: FormState<F>
) => E.Either<FieldError[], T>

export type FieldError = string

export const useFormState = <T extends object>({
  initialValues,
  validators,
}: UseFormStateOptions<T>): UseFormStateHook<T> => {
  const initialState = useMemo(() => createInitialState(initialValues), [
    initialValues,
  ])
  const [currentState, setState] = useState(initialState)

  const set: FormSetFunction<T> = (key, value) => {
    const newState = updatedField(key, value, validators, currentState)
    return setState(softValidation(validators, newState))
  }

  const touch: FormTouchFunction<T> = (key) =>
    setState(touched(key, validators, currentState))

  const fieldProps: FormFieldPropsFunction<T> = (key) => ({
    value: currentState[key].currentValue,
    onChange: (value) => set(key, value),
    onBlur: () => touch(key),
    error: currentState[key].errors.join(", "),
  })

  return { state: currentState, touch, set, fieldProps }
}

const createInitialState = <T extends object>(values: T): FormState<T> =>
  pipe(
    Object.entries(values),
    A.map(([key, value]): [string, FieldState<typeof value>] => [
      key,
      {
        initialValue: value,
        currentValue: value,
        touched: false,
        errors: [],
      },
    ]),
    (entries) => fromEntries(entries) as FormState<T>
  )

const touched = <T extends object>(
  key: keyof T,
  validators: FormValidators<T>,
  state: FormState<T>
): FormState<T> =>
  validateField(key, validators, {
    ...state,
    [key]: {
      ...state[key],
      touched: true,
    },
  })

const updatedField = <K extends keyof T, T extends object>(
  key: K,
  value: T[K],
  validators: FormValidators<T>,
  state: FormState<T>
): FormState<T> =>
  validateField(key, validators, {
    ...state,
    [key]: {
      ...state[key],
      currentValue: value,
      touched: true,
    },
  })

const validateField = <K extends keyof T, T extends object>(
  key: K,
  validators: FormValidators<T>,
  state: FormState<T>
): FormState<T> => {
  const value = state[key].currentValue
  const fieldErrors = pipe(
    validators[key] || [],
    A.map((validate) => validate(value, state)),
    A.separate,
    Separated.left
  )

  return {
    ...state,
    [key]: {
      ...state[key],
      errors: fieldErrors,
    },
  }
}

const softValidation = <T extends object>(
  validators: FormValidators<T>,
  inputState: FormState<T>
): FormState<T> => {
  let state = inputState
  for (const key in inputState) {
    if (inputState[key].touched) {
      state = updatedField(key, inputState[key].currentValue, validators, state)
    }
  }
  return state
}
